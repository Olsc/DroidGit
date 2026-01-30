package com.olsc.droidgit.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import com.olsc.droidgit.business.RepositoryManager;
import com.olsc.droidgit.data.model.GitRepository;
import com.olsc.droidgit.util.NetworkUtils;
import com.olsc.droidgit.util.Constants;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

import android.preference.PreferenceManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.util.TreeMap;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.lib.PersonIdent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * HTTP Git服务器
 * 提供Git HTTP Smart Protocol支持，允许通过HTTP进行git clone/push/pull操作
 * 同时提供Web控制台用于仓库管理
 */
public class GitServer extends NanoHTTPD {
    private final static String TAG = GitServer.class.getSimpleName();
    private final Context context;
    private final RepositoryManager repositoryManager;

    public GitServer(Context context, int port) {
        super(port);
        this.context = context;
        this.repositoryManager = new RepositoryManager(context);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        Log.i(TAG, "Incoming request: " + method + " " + uri);

        if (uri.endsWith("/info/refs")) {
            return handleInfoRefs(session, uri);
        } else if (uri.endsWith("/git-upload-pack")) {
            return handleGitUploadPack(session, uri);
        } else if (uri.endsWith("/git-receive-pack")) {
            return handleGitReceivePack(session, uri);
        }

        if (uri.equals("/") || uri.equals("/index.html")) {
            return serveStaticIndex();
        } else if (uri.equals("/api/repositories") && method == Method.GET) {
            return listRepositories();
        } else if (uri.equals("/api/repositories/create") && method == Method.POST) {
            return createRepository(session);
        } else if (uri.startsWith("/api/repositories/archive/") && method == Method.POST) {
            return archiveRepository(uri);
        } else if (uri.equals("/api/repositories/update") && method == Method.POST) {
            return updateRepository(session);
        } else if (uri.startsWith("/browse/")) {
            return serveRepoBrowser(session, uri);
        } else if (uri.startsWith("/commits/")) {
            return serveRepoCommits(session, uri);
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    private String extractRepoName(String uri) {

        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        int dotGitIndex = uri.indexOf(".git");
        if (dotGitIndex > 0) {
            return uri.substring(0, dotGitIndex);
        }

        int slashIndex = uri.indexOf("/");
        return slashIndex > 0 ? uri.substring(0, slashIndex) : uri;
    }

    private Response handleInfoRefs(IHTTPSession session, String uri) {
        String service = session.getParms().get("service");
        String repoName = extractRepoName(uri);

        Log.d(TAG, "Git info/refs - repo: " + repoName + ", service: " + service);

        if (service == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Service parameter required");
        }

        try {
            GitRepository dbRepo = repositoryManager.getRepositoryByMapping(repoName);
            if (dbRepo != null && dbRepo.isArchived()) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Repository is archived");
            }
            org.eclipse.jgit.lib.Repository repo = repositoryManager.openJGitRepository(repoName);
            if (repo == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Repository not found");
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

            String serviceLine = "# service=" + service + "\n";
            writePacketLine(out, serviceLine);
            out.write("0000".getBytes("UTF-8"));

            if ("git-upload-pack".equals(service)) {
                org.eclipse.jgit.transport.UploadPack uploadPack = new org.eclipse.jgit.transport.UploadPack(repo);
                uploadPack.sendAdvertisedRefs(new org.eclipse.jgit.transport.RefAdvertiser.PacketLineOutRefAdvertiser(
                        new org.eclipse.jgit.transport.PacketLineOut(out)));
            } else if ("git-receive-pack".equals(service)) {
                org.eclipse.jgit.transport.ReceivePack receivePack = new org.eclipse.jgit.transport.ReceivePack(repo);
                receivePack.sendAdvertisedRefs(new org.eclipse.jgit.transport.RefAdvertiser.PacketLineOutRefAdvertiser(
                        new org.eclipse.jgit.transport.PacketLineOut(out)));
            } else {
                repo.close();
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Unknown service");
            }

            repo.close();

            String contentType = "application/x-" + service + "-advertisement";
            return newFixedLengthResponse(Response.Status.OK, contentType,
                    new java.io.ByteArrayInputStream(out.toByteArray()), out.size());
        } catch (Exception e) {
            Log.e(TAG, "Error handling info/refs", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    private void writePacketLine(java.io.OutputStream out, String line) throws IOException {
        byte[] lineBytes = line.getBytes("UTF-8");
        int len = lineBytes.length + 4;
        String hex = String.format("%04x", len);
        out.write(hex.getBytes("UTF-8"));
        out.write(lineBytes);
    }

    private Response handleGitUploadPack(IHTTPSession session, String uri) {
        String repoName = extractRepoName(uri);
        Log.d(TAG, "Git upload-pack (fetch/clone) - repo: " + repoName);

        try {
            org.eclipse.jgit.lib.Repository repo;
            try {
                GitRepository dbRepo = repositoryManager.getRepositoryByMapping(repoName);
                if (dbRepo != null && dbRepo.isArchived()) {
                    return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Repository is archived");
                }
                repo = repositoryManager.openJGitRepository(repoName);
            } catch (Exception e) {
                repo = null;
            }

            if (repo == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Repository not found");
            }

            String contentLengthStr = session.getHeaders().get("content-length");
            String transferEncoding = session.getHeaders().get("transfer-encoding");
            java.io.InputStream input;

            if (transferEncoding != null && transferEncoding.toLowerCase().contains("chunked")) {
                input = new ChunkedInputStream(session.getInputStream());
            } else if (contentLengthStr != null) {
                long contentLength = Long.parseLong(contentLengthStr);
                input = new BoundedInputStream(session.getInputStream(), contentLength);
            } else {
                input = session.getInputStream();
            }
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

            org.eclipse.jgit.transport.UploadPack uploadPack = new org.eclipse.jgit.transport.UploadPack(repo);
            uploadPack.setBiDirectionalPipe(false);
            uploadPack.upload(input, output, null);

            repo.close();

            return newFixedLengthResponse(Response.Status.OK, "application/x-git-upload-pack-result",
                    new java.io.ByteArrayInputStream(output.toByteArray()), output.size());
        } catch (Exception e) {
            Log.e(TAG, "Error handling git-upload-pack", e);
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString());
                sb.append("\n");
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    e.getMessage() + "\n" + sb.toString());
        }
    }

    private Response handleGitReceivePack(IHTTPSession session, String uri) {
        String repoName = extractRepoName(uri);
        Log.d(TAG, "Git receive-pack (push) - repo: " + repoName);

        try {
            org.eclipse.jgit.lib.Repository repo;
            try {
                GitRepository dbRepo = repositoryManager.getRepositoryByMapping(repoName);
                if (dbRepo != null && dbRepo.isArchived()) {
                    return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Repository is archived");
                }
                repo = repositoryManager.openJGitRepository(repoName);
            } catch (Exception e) {
                repo = null;
            }

            if (repo == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Repository not found");
            }

            String contentLengthStr = session.getHeaders().get("content-length");
            String transferEncoding = session.getHeaders().get("transfer-encoding");
            java.io.InputStream input;

            if (transferEncoding != null && transferEncoding.toLowerCase().contains("chunked")) {
                input = new ChunkedInputStream(session.getInputStream());
            } else if (contentLengthStr != null) {
                long contentLength = Long.parseLong(contentLengthStr);
                input = new BoundedInputStream(session.getInputStream(), contentLength);
            } else {
                input = session.getInputStream();
            }
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

            org.eclipse.jgit.transport.ReceivePack receivePack = new org.eclipse.jgit.transport.ReceivePack(repo);
            receivePack.setBiDirectionalPipe(false);
            receivePack.receive(input, output, null);

            repo.close();

            return newFixedLengthResponse(Response.Status.OK, "application/x-git-receive-pack-result",
                    new java.io.ByteArrayInputStream(output.toByteArray()), output.size());
        } catch (Exception e) {
            Log.e(TAG, "Error handling git-receive-pack", e);
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString());
                sb.append("\n");
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    e.getMessage() + "\n" + sb.toString());
        }
    }

    private static class BoundedInputStream extends java.io.InputStream {
        private final java.io.InputStream in;
        private long remaining;

        public BoundedInputStream(java.io.InputStream in, long max) {
            this.in = in;
            this.remaining = max;
        }

        @Override
        public int read() throws java.io.IOException {
            if (remaining <= 0)
                return -1;
            int result = in.read();
            if (result != -1)
                remaining--;
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            if (remaining <= 0)
                return -1;
            if (len > remaining)
                len = (int) remaining;
            int result = in.read(b, off, len);
            if (result != -1)
                remaining -= result;
            return result;
        }

        @Override
        public void close() throws java.io.IOException {
        }
    }

    private static class ChunkedInputStream extends java.io.InputStream {
        private final java.io.InputStream in;
        private long remainingInChunk = 0;
        private boolean initialized = false;
        private boolean eof = false;

        public ChunkedInputStream(java.io.InputStream in) {
            this.in = in;
        }

        private void nextChunk() throws IOException {
            if (eof)
                return;
            if (initialized) {

                int b = in.read();
                if (b == '\r') {
                    b = in.read();
                }
            }

            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\r')
                    continue;
                if (b == '\n')
                    break;
                if (b == ';') {
                    while ((b = in.read()) != -1 && b != '\n')
                        ;
                    break;
                }
                sb.append((char) b);
            }

            if (sb.length() == 0) {
                eof = true;
                return;
            }

            String line = sb.toString().trim();
            if (line.isEmpty()) {
                eof = true;
                return;
            }

            try {
                remainingInChunk = Long.parseLong(line, 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk header: " + line);
            }

            if (remainingInChunk == 0) {
                eof = true;
            }
            initialized = true;
        }

        @Override
        public int read() throws IOException {
            if (eof)
                return -1;
            if (remainingInChunk <= 0) {
                nextChunk();
                if (eof)
                    return -1;
            }
            int data = in.read();
            if (data != -1)
                remainingInChunk--;
            return data;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (eof)
                return -1;
            if (remainingInChunk <= 0) {
                nextChunk();
                if (eof)
                    return -1;
            }

            int toRead = (int) Math.min(len, remainingInChunk);
            int read = in.read(b, off, toRead);
            if (read != -1) {
                remainingInChunk -= read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
        }
    }

    private Response serveStaticIndex() {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>DroidGit Console</title>\n" +
                "    <style>\n" +
                "        :root { --bg: #0A0E14; --surface: #151C24; --accent: #00E5FF; --text: #E0E0E0; --text-dim: #90A4AE; --danger: #FF5252; }\n"
                +
                "        body { font-family: 'Segoe UI', system-ui, sans-serif; background: linear-gradient(135deg, #0A0E14 0%, #1A222C 100%); color: var(--text); margin: 0; min-height: 100vh; }\n"
                +
                "        header { background: rgba(0, 229, 255, 0.05); backdrop-filter: blur(10px); border-bottom: 1px solid rgba(0, 229, 255, 0.2); padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 100; }\n"
                +
                "        .container { max-width: 900px; margin: 0 auto; padding: 24px; }\n" +
                "        .card { background: rgba(255, 255, 255, 0.03); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 8px 32px rgba(0,0,0,0.3); }\n"
                +
                "        .repo-item { border-bottom: 1px solid rgba(255,255,255,0.05); padding: 16px 0; display: flex; justify-content: space-between; align-items: center; transition: 0.3s; }\n"
                +
                "        .repo-item:hover { background: rgba(255,255,255,0.02); padding-left: 8px; }\n" +
                "        .repo-item:last-child { border-bottom: none; }\n" +
                "        .repo-name { font-weight: 600; color: var(--accent); font-size: 1.1em; text-shadow: 0 0 10px rgba(0,229,255,0.3); }\n"
                +
                "        .btn { padding: 8px 20px; font-size: 14px; border-radius: 8px; cursor: pointer; border: 1px solid rgba(255,255,255,0.1); font-weight: 600; transition: all 0.3s; background: rgba(255,255,255,0.05); color: var(--text); text-decoration: none; display: inline-block; }\n"
                +
                "        .btn:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.2); border-color: var(--accent); }\n"
                +
                "        .btn-primary { background: var(--accent); color: var(--bg); border: none; }\n" +
                "        .btn-primary:hover { background: #00B8D4; color: var(--bg); box-shadow: 0 0 15px rgba(0,229,255,0.4); }\n"
                +
                "        .btn-danger { color: var(--danger); }\n" +
                "        .btn-danger:hover { background: rgba(255, 82, 82, 0.1); border-color: var(--danger); }\n" +
                "        .form-group { margin-bottom: 20px; }\n" +
                "        label { display: block; margin-bottom: 8px; color: var(--text-dim); font-size: 0.9em; }\n" +
                "        input { width: 100%; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; box-sizing: border-box; color: white; outline: none; transition: 0.3s; }\n"
                +
                "        input:focus { border-color: var(--accent); box-shadow: 0 0 8px rgba(0,229,255,0.2); }\n" +
                "        .ssh-url { background: rgba(0, 229, 255, 0.1); color: var(--accent); padding: 4px 10px; border-radius: 6px; font-family: 'Consolas', monospace; font-size: 0.85em; display: inline-block; border: 1px solid rgba(0, 229, 255, 0.2); }\n"
                +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <header>\n" +
                "        <span id=\"header-title\" style=\"font-size: 1.2em; font-weight: 700; color: var(--accent); letter-spacing: 1px;\">DroidGit Console</span>\n"
                +
                "    </header>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"card\">\n" +
                "            <div style=\"display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;\">\n"
                +
                "                <h2 id=\"repos-title\" style=\"margin: 0; font-weight: 300;\">Repositories</h2>\n" +
                "                <button id=\"btn-new-repo\" class=\"btn btn-primary\" onclick=\"showCreateForm()\">New Repository</button>\n"
                +
                "            </div>\n" +
                "            <div id=\"repo-list\">Loading...</div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div id=\"create-form\" class=\"card\" style=\"display: none; border-color: var(--accent);\">\n"
                +
                "            <h3 id=\"create-title\" style=\"margin-top: 0; font-weight: 300;\">Create new repository</h3>\n"
                +
                "            <div class=\"form-group\">\n" +
                "                <label id=\"label-name\" for=\"name\">Name</label>\n" +
                "                <input type=\"text\" id=\"name\" placeholder=\"e.g. My Project\">\n" +
                "            </div>\n" +
                "            <div class=\"form-group\">\n" +
                "                <label id=\"label-mapping\" for=\"mapping\">Mapping (URL path)</label>\n" +
                "                <input type=\"text\" id=\"mapping\" placeholder=\"e.g. my-project\">\n" +
                "            </div>\n" +
                "            <div class=\"form-group\">\n" +
                "                <label id=\"label-description\" for=\"description\">Description</label>\n" +
                "                <input type=\"text\" id=\"description\" placeholder=\"Optional description\">\n" +
                "            </div>\n" +
                "            <div style=\"display: flex; gap: 12px;\">\n" +
                "                <button id=\"btn-create\" class=\"btn btn-primary\" onclick=\"createRepo()\">Create</button>\n"
                +
                "                <button id=\"btn-cancel\" class=\"btn\" onclick=\"hideCreateForm()\">Cancel</button>\n"
                +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div id=\"edit-form\" class=\"card\" style=\"display: none; border-color: var(--accent);\">\n"
                +
                "            <h3 id=\"edit-title\" style=\"margin-top: 0; font-weight: 300;\">Edit repository</h3>\n" +
                "            <input type=\"hidden\" id=\"edit-id\">\n" +
                "            <div class=\"form-group\">\n" +
                "                <label id=\"label-edit-name\" for=\"edit-name\">Name</label>\n" +
                "                <input type=\"text\" id=\"edit-name\" disabled style=\"opacity: 0.7; cursor: not-allowed;\">\n"
                +
                "            </div>\n" +
                "            <div class=\"form-group\">\n" +
                "                <label id=\"label-edit-description\" for=\"edit-description\">Description</label>\n" +
                "                <input type=\"text\" id=\"edit-description\" placeholder=\"Optional description\">\n" +
                "            </div>\n" +
                "            <div style=\"display: flex; gap: 12px;\">\n" +
                "                <button id=\"btn-update\" class=\"btn btn-primary\" onclick=\"updateRepo()\">Update</button>\n"
                +
                "                <button id=\"btn-edit-cancel\" class=\"btn\" onclick=\"hideEditForm()\">Cancel</button>\n"
                +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        window.onerror = function(msg, url, line) {\n" +
                "            const list = document.getElementById('repo-list');\n" +
                "            if(list) list.innerHTML += `<div style='color:var(--danger);padding:10px;'>[JS Error] ${msg} (Line: ${line})</div>`;\n"
                +
                "        };\n" +
                "        \n" +
                "        const i18n = {\n" +
                "            en: {\n" +
                "                header: 'DroidGit Web Console',\n" +
                "                repos: 'Repositories',\n" +
                "                newRepo: 'New Repository',\n" +
                "                loading: 'Loading...',\n" +
                "                noRepos: 'No repositories found.',\n" +
                "                createTitle: 'Create new repository',\n" +
                "                name: 'Name',\n" +
                "                mapping: 'Mapping (URL path)',\n" +
                "                description: 'Description',\n" +
                "                create: 'Create',\n" +
                "                cancel: 'Cancel',\n" +
                "                delete: 'Delete',\n" +
                "                archive: 'Archive',\n" +
                "                confirmArchive: 'Are you sure you want to archive {name}? It will become read-only and cloning will be disabled.',\n"
                +
                "                confirmDelete: 'Are you sure you want to delete {name}? This will remove its record in DroidGit, and the actual local repository files will also be permanently deleted.',\n"
                +
                "                fillFields: 'Please fill required fields',\n" +
                "                failCreate: 'Failed to create repository: ',\n" +
                "                failDelete: 'Failed to delete repository',\n" +
                "                failArchive: 'Failed to archive repository',\n" +
                "                browse: 'Browse',\n" +
                "                edit: 'Edit',\n" +
                "                editTitle: 'Edit repository',\n" +
                "                update: 'Update',\n" +
                "                failUpdate: 'Failed to update repository: ',\n" +
                "                error: 'Error: '\n" +
                "            },\n" +
                "            zh: {\n" +
                "                header: 'DroidGit 网页控制台',\n" +
                "                repos: '代码库列表',\n" +
                "                newRepo: '新建代码库',\n" +
                "                loading: '正在扫描硬盘数据...',\n" +
                "                noRepos: '硬盘暂无代码库记录。',\n" +
                "                createTitle: '创建新代码库',\n" +
                "                name: '项目名称(必填)',\n" +
                "                mapping: '映射路径 (URL 路径, 必填)',\n" +
                "                description: '项目描述',\n" +
                "                create: '确认创建',\n" +
                "                cancel: '取消',\n" +
                "                delete: '彻底删除',\n" +
                "                archive: '归档',\n" +
                "                confirmArchive: '确认归档 {name} 吗？归档后代码库将变为只读，且无法进行克隆/拉取操作。',\n" +
                "                confirmDelete: '确认删除 {name} 吗？这将移除其在 DroidGit 中的记录。删除代码库时，本地真实的物理库文件也将被永久删除。',\n" +
                "                fillFields: '请填写完整名称和路径',\n" +
                "                failCreate: '创建失败: ',\n" +
                "                failDelete: '删除失败',\n" +
                "                failArchive: '归档失败',\n" +
                "                browse: '在线浏览',\n" +
                "                edit: '编辑',\n" +
                "                editTitle: '编辑代码库',\n" +
                "                update: '更新',\n" +
                "                failUpdate: '更新失败: ',\n" +
                "                error: '发生错误: '\n" +
                "            }\n" +
                "        };\n" +
                "        \n" +
                "        const lang = navigator.language.startsWith('zh') ? 'zh' : 'en';\n" +
                "        const t = i18n[lang];\n" +
                "\n" +
                "        function showToast(msg) {\n" +
                "            const toast = document.createElement('div');\n" +
                "            toast.style.cssText = 'position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:var(--accent);color:var(--bg);padding:8px 16px;border-radius:20px;font-weight:bold;box-shadow:0 4px 12px rgba(0,0,0,0.3);z-index:9999;transition:opacity 0.3s;pointer-events:none;';\n" +
                "            toast.innerText = msg;\n" +
                "            document.body.appendChild(toast);\n" +
                "            setTimeout(() => {\n" +
                "                toast.style.opacity = '0';\n" +
                "                setTimeout(() => document.body.removeChild(toast), 300);\n" +
                "            }, 2000);\n" +
                "        }\n" +
                "\n" +
                "        function copyToClipboard(text) {\n" +
                "            const successMsg = lang === 'zh' ? '已复制: ' : 'Copied: ';\n" +
                "            if (navigator.clipboard && navigator.clipboard.writeText) {\n" +
                "                navigator.clipboard.writeText(text).then(() => {\n" +
                "                    showToast(successMsg + text);\n" +
                "                }).catch(err => {\n" +
                "                    fallbackCopy(text);\n" +
                "                });\n" +
                "            } else {\n" +
                "                fallbackCopy(text);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function fallbackCopy(text) {\n" +
                "            const successMsg = lang === 'zh' ? '已复制: ' : 'Copied: ';\n" +
                "            const textArea = document.createElement(\"textarea\");\n" +
                "            textArea.value = text;\n" +
                "            textArea.style.position = \"fixed\";\n" +
                "            textArea.style.opacity = \"0\";\n" +
                "            document.body.appendChild(textArea);\n" +
                "            textArea.focus();\n" +
                "            textArea.select();\n" +
                "            try {\n" +
                "                document.execCommand('copy');\n" +
                "                showToast(successMsg + text);\n" +
                "            } catch (err) {\n" +
                "                console.error('Copy failed', err);\n" +
                "                alert('Failed to copy / 复制失败');\n" +
                "            }\n" +
                "            document.body.removeChild(textArea);\n" +
                "        }\n" +
                "\n" +
                "\n" +
                "        document.title = t.header;\n" +
                "        document.getElementById('header-title').innerText = t.header;\n" +
                "        document.getElementById('repos-title').innerText = t.repos;\n" +
                "        document.getElementById('btn-new-repo').innerText = t.newRepo;\n" +
                "        document.getElementById('repo-list').innerText = t.loading;\n" +
                "        document.getElementById('create-title').innerText = t.createTitle;\n" +
                "        document.getElementById('label-name').innerText = t.name;\n" +
                "        document.getElementById('label-mapping').innerText = t.mapping;\n" +
                "        document.getElementById('label-description').innerText = t.description;\n" +
                "        document.getElementById('btn-create').innerText = t.create;\n" +
                "        document.getElementById('btn-cancel').innerText = t.cancel;\n" +
                "        \n" +
                "        document.getElementById('edit-title').innerText = t.editTitle;\n" +
                "        document.getElementById('label-edit-name').innerText = t.name;\n" +
                "        document.getElementById('label-edit-description').innerText = t.description;\n" +
                "        document.getElementById('btn-update').innerText = t.update;\n" +
                "        document.getElementById('btn-edit-cancel').innerText = t.cancel;\n" +
                "\n" +
                "        function translateError(msg) {\n" +
                "            if (lang !== 'zh' || !msg) return msg;\n" +
                "            if (msg.includes('Repository mapping already exists')) return '映射路径已存在' + (msg.includes(':') ? ': ' + msg.split(':')[1] : '');\n"
                +
                "            if (msg.includes('Repository name cannot be empty')) return '项目名称不能为空';\n" +
                "            if (msg.includes('Repository mapping cannot be empty')) return '映射路径不能为空';\n" +
                "            if (msg.includes('Repository not found')) return '找不到该仓库';\n" +
                "            if (msg.includes('Database error')) return '数据库操作错误';\n" +
                "            if (msg.includes('Repository is archived')) return '代码库已归档，禁止操作';\n" +
                "            return msg;\n" +
                "        }\n" +
                "        \n" +
                "        function showCreateForm() { document.getElementById('create-form').style.display = 'block'; hideEditForm(); }\n"
                +
                "        function hideCreateForm() { document.getElementById('create-form').style.display = 'none'; }\n"
                +
                "        \n" +
                "        function showEditForm(id, name, description) {\n" +
                "            document.getElementById('create-form').style.display = 'none';\n" +
                "            document.getElementById('edit-form').style.display = 'block';\n" +
                "            document.getElementById('edit-id').value = id;\n" +
                "            document.getElementById('edit-name').value = name;\n" +
                "            document.getElementById('edit-description').value = description;\n" +
                "            window.scrollTo(0, 0);\n" +
                "        }\n" +
                "        function hideEditForm() { document.getElementById('edit-form').style.display = 'none'; }\n" +
                "        \n" +
                "\n" +
                "        document.getElementById('name').addEventListener('input', function(e) {\n" +
                "            const mappingInput = document.getElementById('mapping');\n" +
                "            if (mappingInput.dataset.manual !== \"true\") {\n" +
                "                const val = e.target.value.trim();\n" +
                "                mappingInput.value = val.toLowerCase().replace(/\\s+/g, '-').replace(/[^a-z0-9-]/g, '');\n"
                +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        document.getElementById('mapping').addEventListener('input', function(e) {\n" +
                "            e.target.dataset.manual = \"true\";\n" +
                "        });\n" +
                "        \n" +
                "        async function loadRepos() {\n" +
                "            const list = document.getElementById('repo-list');\n" +
                "            try {\n" +
                "                const controller = new AbortController();\n" +
                "                const timeoutId = setTimeout(() => controller.abort(), 10000);\n" +
                "                const resp = await fetch('/api/repositories', { signal: controller.signal });\n" +
                "                clearTimeout(timeoutId);\n" +
                "                if (!resp.ok) {\n" +
                "                    const text = await resp.text();\n" +
                "                    throw new Error(`Server Error (${resp.status}): ${text}`);\n" +
                "                }\n" +
                "                const repos = await resp.json();\n" +
                "                console.log('Loaded repos:', repos);\n" +
                "                if (!Array.isArray(repos)) throw new Error('Invalid JSON response');\n" +
                "                list.innerHTML = repos.length ? '' : `<div style='text-align:center;color:var(--text-dim);padding:20px;'>${t.noRepos}</div>`;\n"
                +
                "                repos.forEach(repo => {\n" +
                "                    const item = document.createElement('div');\n" +
                "                    item.className = 'repo-item';\n" +
                "                    const mapping = repo.mapping || '';\n" +
                "                    const mappingUrl = mapping.endsWith('.git') ? mapping : mapping + '.git';\n" +
                "                    const safeName = (repo.name || '').replace(/'/g, \"\\\\'\");\n" +
                "                    const safeDesc = (repo.description || '').replace(/'/g, \"\\\\'\").replace(/\\n/g, '<br>');\n"
                +
                "                    item.innerHTML = `\n" +
                "                        <div>\n" +
                "                            <div class=\"repo-name\">\n" +
                "                                ${repo.name || 'Unnamed'}\n" +
                "                                ${repo.archived ? '<span style=\"font-size:0.7em;background:#FF9800;color:black;padding:2px 6px;border-radius:4px;margin-left:8px;vertical-align:middle;\">ARCHIVED</span>' : ''}\n"
                +
                "                            </div>\n" +
                "                            <div style=\"color:var(--text-dim);font-size:0.9em;margin-top:4px;margin-bottom:8px;\">${repo.description || ''}</div>\n"
                +
                "                            <div style=\"${repo.archived ? 'display:none;' : ''}\">\n" +
                "                                <span class=\"ssh-url\" onclick=\"copyToClipboard(this.innerText)\" style=\"cursor:pointer\" title=\"Click to copy\">http://${location.host}/${mappingUrl}</span>\n"
                +
                "                            </div>\n" +
                "                        </div>\n" +
                "                        <div style=\"display:flex;gap:8px;\">\n" +
                "                            <a href=\"/browse/${mapping}\" class=\"btn\" style=\"white-space:nowrap;\">${t.browse}</a>\n"
                +
                "                            <button class=\"btn\" onclick=\"showEditForm('${repo.id}', '${safeName}', '${safeDesc}')\">${t.edit}</button>\n"
                +
                "                            ${!repo.archived ? `<button class=\"btn btn-danger\" onclick=\"archiveRepo('${repo.id}', '${safeName}')\">${t.archive}</button>` : ''}\n"
                +
                "                        </div>\n" +
                "                    `;\n" +
                "                    list.appendChild(item);\n" +
                "                });\n" +
                "            } catch(e) {\n" +
                "                console.error('loadRepos Error:', e);\n" +
                "                let errorMsg = e.name === 'AbortError' ? 'Request timed out / 请求超时' : e.message;\n" +
                "                list.innerHTML = `<div style='text-align:center;color:var(--danger);padding:20px;'>\n"
                +
                "                    <div style='font-weight:bold;margin-bottom:10px;'>${t.error || 'Error: '}</div>\n"
                +
                "                    <div style='font-family:monospace;font-size:0.85em;background:rgba(0,0,0,0.2);padding:10px;border-radius:4px;'>${errorMsg}</div>\n"
                +
                "                    <button class='btn btn-primary' style='margin-top:20px' onclick='loadRepos()'>Retry / 重试</button>\n"
                +
                "                </div>`;\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        async function createRepo() {\n" +
                "            const name = document.getElementById('name').value;\n" +
                "            const mapping = document.getElementById('mapping').value;\n" +
                "            const description = document.getElementById('description').value;\n" +
                "            if(!name || !mapping) { alert(t.fillFields); return; }\n" +
                "            \n" +
                "            const resp = await fetch('/api/repositories/create', {\n" +
                "                method: 'POST',\n" +
                "                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "                body: new URLSearchParams({ name, mapping, description })\n" +
                "            });\n" +
                "            if (resp.ok) {\n" +
                "                document.getElementById('name').value = '';\n" +
                "                document.getElementById('mapping').value = '';\n" +
                "                document.getElementById('description').value = '';\n" +
                "                document.getElementById('mapping').dataset.manual = \"false\";\n" +
                "                hideCreateForm();\n" +
                "                loadRepos();\n" +
                "            } else {\n" +
                "                const msg = await resp.text();\n" +
                "                alert(t.failCreate + translateError(msg));\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        async function updateRepo() {\n" +
                "            const id = document.getElementById('edit-id').value;\n" +
                "            const description = document.getElementById('edit-description').value;\n" +
                "            \n" +
                "            const resp = await fetch('/api/repositories/update', {\n" +
                "                method: 'POST',\n" +
                "                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "                body: new URLSearchParams({ id, description })\n" +
                "            });\n" +
                "            \n" +
                "            if (resp.ok) {\n" +
                "                hideEditForm();\n" +
                "                loadRepos();\n" +
                "            } else {\n" +
                "                const msg = await resp.text();\n" +
                "                alert(t.failUpdate + translateError(msg));\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        async function archiveRepo(id, name) {\n" +
                "            if (!confirm(t.confirmArchive.replace('{name}', name))) return;\n" +
                "            const resp = await fetch('/api/repositories/archive/' + id, { method: 'POST' });\n" +
                "            if (resp.ok) loadRepos();\n" +
                "            else {\n" +
                "                const errorText = await resp.text();\n" +
                "                alert(t.failArchive + (errorText ? ': ' + translateError(errorText) : ''));\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        loadRepos();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
        return newFixedLengthResponse(html);
    }

    private Response listRepositories() {
        Log.i(TAG, "Handling API request: /api/repositories");
        try {
            List<GitRepository> repos = repositoryManager.getAllRepositories();
            Log.i(TAG, "Found " + repos.size() + " repositories in database");

            StringBuilder json = new StringBuilder("[");

            // 鲁棒的端口读取方案，与ServerService保持一致
            int httpPort = Constants.Prefs.DEFAULT_HTTP_PORT;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            try {
                String portStr = prefs.getString(Constants.Prefs.HTTP_PORT,
                        String.valueOf(Constants.Prefs.DEFAULT_HTTP_PORT));
                httpPort = Integer.parseInt(portStr);
            } catch (ClassCastException e) {
                try {
                    httpPort = prefs.getInt(Constants.Prefs.HTTP_PORT, Constants.Prefs.DEFAULT_HTTP_PORT);
                } catch (Exception ex) {
                    httpPort = Constants.Prefs.DEFAULT_HTTP_PORT;
                }
            } catch (Exception e) {
                httpPort = Constants.Prefs.DEFAULT_HTTP_PORT;
            }

            boolean first = true;
            for (int i = 0; i < repos.size(); i++) {
                GitRepository r = repos.get(i);

                if (!r.isActive()) {
                    continue;
                }

                if (!first) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"id\":").append(r.getId()).append(",")
                        .append("\"name\":\"").append(escapeJson(r.getName())).append("\",")
                        .append("\"mapping\":\"").append(escapeJson(r.getMapping())).append("\",")
                        .append("\"description\":\"").append(escapeJson(r.getDescription())).append("\",")
                        .append("\"archived\":").append(r.isArchived()).append(",")
                        .append("\"httpPort\":").append(httpPort)
                        .append("}");

                first = false;
            }
            json.append("]");
            Log.i(TAG, "Sending " + repos.size() + " repositories as JSON (" + json.length() + " bytes)");

            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error in listRepositories", e);
            String msg = e.getMessage() != null ? e.getMessage() : "Internal Error";
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, msg);
        }
    }

    private Response createRepository(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            Map<String, String> params = session.getParms();
            String name = params.get("name");
            String mapping = params.get("mapping");
            String description = params.get("description");
            if (description == null)
                description = "";

            Log.d(TAG, "Create Repo - Name: " + name + ", Mapping: " + mapping);

            if (name == null || "".equals(name.trim()) || mapping == null || "".equals(mapping.trim())) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Missing parameters");
            }

            // 使用RepositoryManager创建仓库（会同时创建数据库记录和物理Git仓库）
            repositoryManager.createRepository(name, mapping, description);

            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK");
        } catch (RepositoryManager.RepositoryException e) {
            Log.e(TAG, "Error in createRepository", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Internal Error";
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, errorMsg);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in createRepository", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                    "Internal Server Error");
        }
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private Response archiveRepository(String uri) {
        try {
            String idStr = uri.substring(uri.lastIndexOf("/") + 1);
            int id = Integer.parseInt(idStr);

            // 使用RepositoryManager归档仓库
            repositoryManager.archiveRepository(id);

            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK");
        } catch (RepositoryManager.RepositoryException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }
    }

    private Response updateRepository(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            Map<String, String> params = session.getParms();
            String idStr = params.get("id");
            String description = params.get("description");

            if (idStr == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing ID");
            }

            int id = Integer.parseInt(idStr);

            // 使用RepositoryManager更新仓库
            repositoryManager.updateRepository(id, description);

            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK");
        } catch (RepositoryManager.RepositoryException e) {
            Log.e(TAG, "Error in updateRepository", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateRepository", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }
    }

    private boolean isChinese(IHTTPSession session) {
        String acceptLanguage = session.getHeaders().get("accept-language");
        return acceptLanguage != null && acceptLanguage.toLowerCase().contains("zh");
    }

    private String getCommonStyles(boolean isZh) {
        return "<style>" +
                ":root { --bg: #0A0E14; --surface: #151C24; --accent: #00E5FF; --text: #E0E0E0; --text-dim: #90A4AE; --danger: #FF5252; --card-bg: rgba(255, 255, 255, 0.03); }"
                +
                "body{font-family:'Segoe UI',system-ui,sans-serif;margin:0;background:linear-gradient(135deg,#0A0E14 0%,#1A222C 100%);color:var(--text);min-height:100vh;}"
                +
                ".container{max-width:900px;margin:0 auto;padding:24px;}" +
                ".card{background:var(--card-bg);backdrop-filter:blur(12px);border:1px solid rgba(255,255,255,0.1);border-radius:16px;padding:24px;margin-bottom:24px;box-shadow:0 8px 32px rgba(0,0,0,0.5);}"
                +
                "a{color:var(--accent);text-decoration:none;transition:0.3s;}a:hover{text-shadow:0 0 8px rgba(0,229,255,0.5);}"
                +
                "table{width:100%;border-collapse:collapse;}td,th{padding:14px 10px;text-align:left;border-bottom:1px solid rgba(255,255,255,0.05);}"
                +
                "th{color:var(--text-dim);font-weight:400;font-size:0.85em;text-transform:uppercase;letter-spacing:1.5px;}"
                +
                "tr:hover td{background:rgba(255,255,255,0.01);}" +
                ".header{margin-bottom:32px;display:flex;justify-content:space-between;align-items:center;}" +
                "select{background:rgba(0,0,0,0.3);color:white;border:1px solid rgba(255,255,255,0.1);padding:8px 16px;border-radius:8px;outline:none;cursor:pointer;}"
                +
                ".btn-back{background:rgba(255,255,255,0.05);padding:10px 20px;border-radius:10px;border:1px solid rgba(255,255,255,0.1);color:var(--text);font-weight:600;transition:0.3s;text-decoration:none;}"
                +
                ".btn-back:hover{background:rgba(255,255,255,0.1);border-color:var(--accent);box-shadow:0 0 15px rgba(0,229,255,0.2);}"
                +
                "</style>";
    }

    private Response serveRepoBrowser(IHTTPSession session, String uri) {
        boolean isZh = isChinese(session);

        Map<String, String> parms = session.getParms();
        boolean isRaw = "true".equals(parms.get("raw"));

        String mapping = uri.substring("/browse/".length());
        if (mapping.contains("/"))
            mapping = mapping.split("/")[0];

        String refName = parms.containsKey("ref") ? parms.get("ref") : "HEAD";
        String path = parms.containsKey("path") ? parms.get("path") : "";
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        org.eclipse.jgit.lib.Repository repo = null;
        try {
            try {
                repo = repositoryManager.openJGitRepository(mapping);
            } catch (Exception e) {
                repo = null;
            }
            if (repo == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                        isZh ? "找不到该仓库" : "Repository not found");
            }

            ObjectId commitId = repo.resolve(refName);
            if (commitId == null) {
                // 检查是否是空仓库
                boolean isEmptyRepo = false;
                try {
                    Map<String, Ref> allRefs = repo.getAllRefs();
                    isEmptyRepo = allRefs.isEmpty();
                } catch (Exception e) {
                    // 忽略检查异常
                }

                repo.close();
                if (isEmptyRepo) {
                    String message = isZh ? "这是一个空的 Git 仓库，请先推送一些代码。"
                            : "This is an empty Git repository. Please push some code first.";
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, message);
                } else {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                            isZh ? "引用未找到" : "Ref not found");
                }
            }

            RevWalk revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree = commit.getTree();
            ObjectId targetId = tree.getId();
            FileMode targetMode = FileMode.TREE;

            if (!path.isEmpty()) {
                TreeWalk tw = TreeWalk.forPath(repo, path, tree);
                if (tw != null) {
                    targetId = tw.getObjectId(0);
                    targetMode = tw.getFileMode(0);
                } else {
                    repo.close();
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                            isZh ? "路径未找到" : "Path not found");
                }
            }

            if (isRaw) {
                if ((targetMode.getBits() & FileMode.TYPE_FILE) == 0
                        && (targetMode.getBits() & FileMode.TYPE_GITLINK) == 0) {
                    repo.close();
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                            isZh ? "不是文件" : "Not a file");
                }

                org.eclipse.jgit.lib.ObjectLoader loader = repo.open(targetId);
                String mime = getMimeType(path);

                return newFixedLengthResponse(Response.Status.OK, mime, loader.openStream(), loader.getSize());
            }

            String title = (isZh ? "浏览 - " : "Browse - ") + mapping;

            StringBuilder html = new StringBuilder();
            html.append(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>")
                    .append(title).append("</title>");
            html.append(getCommonStyles(isZh));
            html.append("<script src=\"https://cdn.jsdelivr.net/npm/marked/marked.min.js\"></script>");
            html.append(
                    "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/atom-one-dark.min.css\">");
            html.append(
                    "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js\"></script>");
            html.append("</head><body><div class='container'>");

            html.append("<div class='header'><h2><a href='/' class='btn-back'>&larr; ").append(isZh ? "返回首页" : "Home")
                    .append("</a> <span style='margin-left:15px;color:white;font-weight:300;'>").append(mapping)
                    .append("</span></h2></div>");

            // 分支列表
            List<String> branches = new ArrayList<>();
            Map<String, Ref> refs = repo.getAllRefs();
            for (String k : refs.keySet()) {
                if (k.startsWith("refs/heads/"))
                    branches.add(k.substring(11));
            }
            Collections.sort(branches);
            String currentBranchShort = refName.startsWith("refs/heads/") ? refName.substring(11) : refName;

            html.append(
                    "<div style='margin-bottom:16px;background:rgba(255,255,255,0.02);padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,0.05);display:flex;align-items:center;flex-wrap:wrap;gap:12px;'>");
            html.append(
                    "<select style='background:#1A222C;border:1px solid rgba(0,229,255,0.3);' onchange=\"location.href='?ref=refs/heads/'+this.value\">");
            for (String b : branches) {
                html.append("<option value='").append(b).append("'")
                        .append(b.equals(currentBranchShort) ? " selected" : "").append(">").append(b)
                        .append("</option>");
            }
            html.append("</select>");

            html.append("<span style='color:var(--text-dim);'>|</span> <a href='/commits/").append(mapping)
                    .append("?ref=").append(refName).append("'>")
                    .append(isZh ? "提交历史" : "History").append("</a>");

            html.append(
                    "<span style='color:var(--text-dim);'>|</span> <span style='font-family:monospace;color:var(--accent);'>");

            html.append("<a href='?ref=").append(refName).append("&path='>root</a>");
            if (!path.isEmpty()) {
                String[] parts = path.split("/");
                String cur = "";
                for (String p : parts) {
                    cur = cur.isEmpty() ? p : cur + "/" + p;
                    html.append(" / <a href='?ref=").append(refName).append("&path=").append(cur).append("'>").append(p)
                            .append("</a>");
                }
            }
            html.append("</span></div>");

            if ((targetMode.getBits() & FileMode.TYPE_TREE) != 0) {
                html.append("<table><thead><tr><th>").append(isZh ? "名称" : "Name").append("</th><th>")
                        .append(isZh ? "类型" : "Type").append("</th><th>").append(isZh ? "大小" : "Size")
                        .append("</th><th>").append(isZh ? "时间" : "Date").append("</th></tr></thead><tbody>");
                if (!path.isEmpty()) {
                    String parent = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
                    html.append("<tr><td><a href='?ref=").append(refName).append("&path=").append(parent)
                            .append("'>..</a></td><td></td><td></td><td></td></tr>");
                }

                TreeWalk treeWalk = new TreeWalk(repo);
                treeWalk.addTree(targetId);
                treeWalk.setRecursive(false);

                String readmeContent = null;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                while (treeWalk.next()) {
                    String name = treeWalk.getNameString();
                    FileMode mode = treeWalk.getFileMode(0);
                    boolean isTree = (mode.getBits() & FileMode.TYPE_TREE) != 0;
                    String fullPath = path.isEmpty() ? name : path + "/" + name;

                    html.append("<tr>");
                    html.append("<td>");
                    if (isTree) {
                        html.append("📂 <a href='?ref=").append(refName).append("&path=").append(fullPath).append("'>")
                                .append(name).append("</a>");
                    } else {
                        html.append("📄 <a href='?ref=").append(refName).append("&path=").append(fullPath).append("'>")
                                .append(name).append("</a>");
                        if (name.equalsIgnoreCase("readme.md")) {
                            org.eclipse.jgit.lib.ObjectLoader loader = repo.open(treeWalk.getObjectId(0));
                            if (loader.getSize() < 1024 * 512) { // 仅加载小于512KB的文件
                                readmeContent = new String(loader.getBytes(), "UTF-8");
                            }
                        }
                    }
                    html.append("</td>");

                    html.append("<td style='color:var(--text-dim);font-size:0.9em;'>")
                            .append(isTree ? (isZh ? "目录" : "DIR") : (isZh ? "文件" : "FILE")).append("</td>");

                    html.append("<td style='color:var(--text-dim);font-size:0.9em;text-align:right;'>");
                    if (!isTree) {
                        try {
                            long size = repo.open(treeWalk.getObjectId(0)).getSize();
                            html.append(formatSize(size));
                        } catch (Exception e) {
                        }
                    }
                    html.append("</td>");

                    html.append("<td style='color:var(--text-dim);font-size:0.9em;text-align:right;'>");
                    Date date = getLastModifiedDate(repo, commitId, fullPath);
                    if (date != null) {
                        html.append(sdf.format(date));
                    }
                    html.append("</td>");

                    html.append("</tr>");
                }
                html.append("</tbody></table></div>");

                // 如果存在README则显示
                if (readmeContent != null) {
                    String safeContent = escapeJson(readmeContent);
                    html.append(
                            "<div class='card' id='readme'><h3>README.md</h3><div id='md-content' class='markdown-body'></div></div>");
                    html.append("<script>\n");
                    html.append("    try {\n");
                    html.append("        const content = marked.parse(\"").append(safeContent).append("\");\n");
                    html.append("        const container = document.getElementById('md-content');\n");
                    html.append("        container.innerHTML = content;\n");
                    html.append("\n");
                    html.append("        const urlParams = new URLSearchParams(window.location.search);\n");
                    html.append("        const ref = urlParams.get('ref') || 'HEAD';\n");
                    html.append("        let basePath = urlParams.get('path') || '';\n");
                    html.append("        if (basePath && !basePath.endsWith('/')) basePath += '/';\n");
                    html.append("\n");
                    html.append("        // 修复链接路径\n");
                    html.append("        const links = container.getElementsByTagName('a');\n");
                    html.append("        for (let link of links) {\n");
                    html.append("            const href = link.getAttribute('href');\n");
                    html.append(
                            "            if (href && !href.match(/^[a-z]+:/) && !href.startsWith('/') && !href.startsWith('#')) {\n");
                    html.append(
                            "                link.href = '?ref=' + encodeURIComponent(ref) + '&path=' + encodeURIComponent(basePath + href);\n");
                    html.append("            }\n");
                    html.append("        }\n");
                    html.append("\n");
                    html.append("        // 修复图片路径\n");
                    html.append("        const imgs = container.getElementsByTagName('img');\n");
                    html.append("        for (let img of imgs) {\n");
                    html.append("            const src = img.getAttribute('src');\n");
                    html.append(
                            "            if (src && !src.match(/^[a-z]+:/) && !src.startsWith('/') && !src.startsWith('data:')) {\n");
                    html.append(
                            "                img.src = '?ref=' + encodeURIComponent(ref) + '&path=' + encodeURIComponent(basePath + src) + '&raw=true';\n");
                    html.append("            }\n");
                    html.append("        }\n");
                    html.append("    } catch (e) { console.error('Error rendering markdown:', e); }\n");
                    html.append("</script>");
                }

            } else {

                org.eclipse.jgit.lib.ObjectLoader loader = repo.open(targetId);
                long size = loader.getSize();
                String rawUrl = "?ref=" + refName + "&path=" + path + "&raw=true";
                String mime = getMimeType(path);
                String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

                html.append("<div class='card'>");
                html.append(
                        "<div style='display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;border-bottom:1px solid rgba(255,255,255,0.1);padding-bottom:12px;'>");
                html.append("<h3>").append(fileName)
                        .append(" <span style='font-weight:400;font-size:0.8em;color:var(--text-dim);'>(")
                        .append(formatSize(size)).append(")</span></h3>");
                html.append("<a href='").append(rawUrl).append("' download class='btn' style='font-size:0.9em;'>")
                        .append(isZh ? "下载文件" : "Download").append("</a>");
                html.append("</div>");

                if (mime.startsWith("image/")) {
                    html.append(
                            "<div style='text-align:center;background:rgba(0,0,0,0.3);padding:20px;border-radius:8px;'>");
                    html.append("<img src='").append(rawUrl)
                            .append("' style='max-width:100%;max-height:80vh;border-radius:4px;'>");
                    html.append("</div>");
                } else if (mime.startsWith("video/")) {
                    html.append("<div style='text-align:center;'>");
                    html.append(
                            "<video controls style='max-width:100%;max-height:80vh;border-radius:8px;'><source src='")
                            .append(rawUrl).append("' type='").append(mime).append("'></video>");
                    html.append("</div>");
                } else if (mime.startsWith("audio/")) {
                    html.append("<div style='padding:20px;background:rgba(0,0,0,0.2);border-radius:8px;'>");
                    html.append("<audio controls style='width:100%;'><source src='").append(rawUrl).append("' type='")
                            .append(mime).append("'></audio>");
                    html.append("</div>");
                } else if (isText(mime) || size < 1024 * 1024) {
                    if (size > 1024 * 512) {
                        html.append("<div style='color:var(--danger);margin-bottom:10px;'>")
                                .append(isZh ? "文件过大，仅显示前 512KB" : "File too large, showing first 512KB")
                                .append("</div>");
                    }

                    byte[] bytes;
                    try (java.io.InputStream is = loader.openStream()) {
                        int readSize = (int) Math.min(size, 1024 * 512);
                        bytes = new byte[readSize];
                        int read = 0;
                        while (read < readSize) {
                            int r = is.read(bytes, read, readSize - read);
                            if (r == -1)
                                break;
                            read += r;
                        }
                    }
                    String content = new String(bytes, "UTF-8").replace("<", "&lt;").replace(">", "&gt;");
                    html.append("<pre><code class='hljs'>").append(content).append("</code></pre>");
                    html.append("<script>hljs.highlightAll();</script>");
                } else {
                    html.append("<div style='padding:40px;text-align:center;color:var(--text-dim);'>")
                            .append(isZh ? "此文件不支持预览" : "No preview available for this file type").append("</div>");
                }

                html.append("</div>");
            }

            html.append("</div></body></html>");
            repo.close();
            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, html.toString());

        } catch (

        Exception e) {
            if (repo != null)
                repo.close();
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.toString());
        }
    }

    private Response serveRepoCommits(IHTTPSession session, String uri) {
        boolean isZh = isChinese(session);
        String mapping = uri.substring("/commits/".length());
        if (mapping.contains("/"))
            mapping = mapping.split("/")[0];

        Map<String, String> parms = session.getParms();
        String refName = parms.containsKey("ref") ? parms.get("ref") : "HEAD";

        String title = (isZh ? "提交历史 - " : "History - ") + mapping;

        StringBuilder html = new StringBuilder();
        html.append(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>")
                .append(title).append("</title>");
        html.append(getCommonStyles(isZh));
        html.append("<style>.time{color:var(--text-dim);font-size:0.85em;font-family:monospace;}</style>");
        html.append("</head><body><div class='container'>");
        html.append("<div class='header'><h2><a href='/browse/").append(mapping).append("?ref=").append(refName)
                .append("' class='btn-back'>&larr; ").append(isZh ? "返回目录" : "Files")
                .append("</a> <span style='margin-left:15px;color:white;font-weight:300;'>")
                .append(isZh ? "提交记录" : "Commit History").append("</span></h2></div>");

        org.eclipse.jgit.lib.Repository repo = null;
        try {
            try {
                repo = repositoryManager.openJGitRepository(mapping);
            } catch (Exception e) {
                repo = null;
            }
            if (repo == null) {
                html.append("<div class='card'>").append(isZh ? "未找到仓库" : "Repository not found")
                        .append("</div></div></body></html>");
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_HTML, html.toString());
            }

            ObjectId commitId = repo.resolve(refName);
            if (commitId == null) {
                // 检查是否是空仓库
                boolean isEmptyRepo = false;
                try {
                    Map<String, Ref> allRefs = repo.getAllRefs();
                    isEmptyRepo = allRefs.isEmpty();
                } catch (Exception e) {
                    // 忽略检查异常
                }

                String emptyTitle = isZh ? "空仓库" : "Empty Repository";
                String emptyMessage = isZh ? "这是一个全新的 Git 仓库" : "This is a brand new Git repository";

                String httpUrl = "http://" + session.getHeaders().get("host") + "/"
                        + (mapping.endsWith(".git") ? mapping : mapping + ".git");

                html.append("<div class='card' style='text-align:center;padding:40px;'>");
                html.append("<div style='font-size:3em;margin-bottom:20px;'>📂</div>");
                html.append("<h2 style='margin-top:0;color:var(--accent);'>").append(emptyTitle).append("</h2>");
                html.append("<p style='color:var(--text-dim);margin-bottom:30px;'>").append(emptyMessage)
                        .append("</p>");

                html.append(
                        "<div style='text-align:left;background:rgba(0,0,0,0.3);padding:20px;border-radius:8px;font-family:monospace;font-size:0.9em;margin:0 auto;max-width:600px;'>");
                html.append("<div style='color:#80CBC4;margin-bottom:8px;'># Command line instructions</div>");
                html.append("<div style='color:var(--text-dim);'>git init</div>");
                html.append("<div style='color:var(--text-dim);'>git add .</div>");
                html.append("<div style='color:var(--text-dim);'>git commit -m \"Initial commit\"</div>");
                html.append(
                        "<div style='color:var(--text-dim);'>git remote add origin <span style='color:var(--accent);'>")
                        .append(httpUrl).append("</span></div>");
                html.append("<div style='color:var(--text-dim);'>git push -u origin master</div>");
                html.append("</div>");

                html.append("</div></div></body></html>");
                repo.close();
                return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, html.toString());
            }

            html.append("<div class='card'><table>");
            html.append("<thead><tr><th>").append(isZh ? "提交信息" : "Message").append("</th><th>")
                    .append(isZh ? "作者" : "Author").append("</th><th>").append(isZh ? "日期" : "Date")
                    .append("</th></tr></thead><tbody>");

            RevWalk revWalk = new RevWalk(repo);
            revWalk.markStart(revWalk.parseCommit(commitId));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            int count = 0;
            for (RevCommit commit : revWalk) {
                if (count++ > 100)
                    break; // 限制为100次提交

                PersonIdent author = commit.getAuthorIdent();
                html.append("<tr>");
                html.append("<td><b>").append(commit.getShortMessage()).append("</b><br><span class='time'>")
                        .append(commit.getName().substring(0, 7)).append("</span></td>");
                html.append("<td>").append(author.getName()).append("</td>");
                html.append("<td>").append(sdf.format(author.getWhen())).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody></table></div>");
            repo.close();

        } catch (Exception e) {
            html.append("<div class='card'>Error: ").append(e.getMessage()).append("</div>");
            if (repo != null)
                repo.close();
        }

        html.append("</div></body></html>");
        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, html.toString());
    }

    @Override
    public void stop() {
        super.stop();
        if (repositoryManager != null) {
            repositoryManager.close();
        }
    }

    private String formatSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private boolean isText(String mime) {
        return mime.startsWith("text/") || mime.equals("application/json") || mime.equals("application/xml")
                || mime.equals("application/javascript");
    }

    private String getMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm"))
            return "text/html";
        if (lower.endsWith(".css"))
            return "text/css";
        if (lower.endsWith(".js"))
            return "application/javascript";
        if (lower.endsWith(".json"))
            return "application/json";
        if (lower.endsWith(".xml"))
            return "text/xml";
        if (lower.endsWith(".txt"))
            return "text/plain";
        if (lower.endsWith(".md"))
            return "text/markdown";
        if (lower.endsWith(".java"))
            return "text/x-java-source";
        if (lower.endsWith(".c") || lower.endsWith(".h") || lower.endsWith(".cpp"))
            return "text/x-c";
        if (lower.endsWith(".py"))
            return "text/x-python";
        if (lower.endsWith(".sh"))
            return "text/x-script.sh";

        if (lower.endsWith(".png"))
            return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".gif"))
            return "image/gif";
        if (lower.endsWith(".webp"))
            return "image/webp";
        if (lower.endsWith(".svg"))
            return "image/svg+xml";

        if (lower.endsWith(".mp4"))
            return "video/mp4";
        if (lower.endsWith(".webm"))
            return "video/webm";
        if (lower.endsWith(".ogg"))
            return "video/ogg";

        if (lower.endsWith(".mp3"))
            return "audio/mpeg";
        if (lower.endsWith(".wav"))
            return "audio/wav";

        return "application/octet-stream";
    }

    private Date getLastModifiedDate(org.eclipse.jgit.lib.Repository repo, ObjectId commitId, String path) {
        try (RevWalk rw = new RevWalk(repo)) {
            rw.markStart(rw.parseCommit(commitId));
            if (path != null && !path.isEmpty()) {
                rw.setTreeFilter(AndTreeFilter.create(PathFilter.create(path), TreeFilter.ANY_DIFF));
            }
            java.util.Iterator<RevCommit> it = rw.iterator();
            if (it.hasNext()) {
                return it.next().getAuthorIdent().getWhen();
            }
        } catch (Exception e) {
        }
        return null;
    }
}
