package com.olsc.droidgit.business;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.olsc.droidgit.data.database.DatabaseManager;
import com.olsc.droidgit.data.model.GitRepository;
import com.olsc.droidgit.util.Constants;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 仓库业务管理器
 * 统一处理仓库的创建、删除、查询等业务逻辑
 * 整合了数据库操作和Git文件系统操作
 */
public class RepositoryManager {
    private static final String TAG = "RepositoryManager";

    private final Context context;
    private final DatabaseManager dbManager;
    private final String repositoriesBasePath;

    public RepositoryManager(Context context) {
        this.context = context;
        this.dbManager = DatabaseManager.getInstance(context);

        // 获取仓库存储路径
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.repositoriesBasePath = prefs.getString(
                Constants.Prefs.GIT_ROOT_DIR,
                Constants.Prefs.DEFAULT_GIT_ROOT_DIR);

        // 确保仓库目录存在
        File baseDir = new File(repositoriesBasePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
            Log.i(TAG, "Created repositories directory: " + repositoriesBasePath);
        }
    }

    /**
     * 创建新仓库
     * 
     * @param name        仓库名称
     * @param mapping     URL映射路径
     * @param description 描述
     * @return 创建的仓库对象
     */
    public GitRepository createRepository(String name, String mapping, String description)
            throws RepositoryException {

        // 验证参数
        if (name == null || name.trim().isEmpty()) {
            throw new RepositoryException("Repository name cannot be empty");
        }
        if (mapping == null || mapping.trim().isEmpty()) {
            throw new RepositoryException("Repository mapping cannot be empty");
        }

        try {
            // 检查映射是否已存在
            List<GitRepository> existing = dbManager.getRepositoryDao()
                    .queryForEq("mapping", mapping);
            if (!existing.isEmpty()) {
                throw new RepositoryException("Repository mapping already exists: " + mapping);
            }

            // 创建数据库记录
            GitRepository repository = new GitRepository(name, mapping, description);
            dbManager.getRepositoryDao().create(repository);

            // 创建物理Git仓库
            File repoDir = getRepositoryPath(mapping);
            try {
                Git git = Git.init()
                        .setDirectory(repoDir)
                        .setBare(true)
                        .call();
                git.close();
                Log.i(TAG, "Created Git repository: " + repoDir.getAbsolutePath());
            } catch (GitAPIException e) {
                // 回滚数据库操作
                dbManager.getRepositoryDao().delete(repository);
                throw new RepositoryException("Failed to create Git repository", e);
            }

            return repository;

        } catch (SQLException e) {
            throw new RepositoryException("Database error while creating repository", e);
        }
    }

    /**
     * 导入现有仓库（仅在数据库中注册，不创建物理文件）
     * 
     * @param folderName 物理文件夹名称
     * @return 导入的仓库对象，如果已存在或无效则返回null
     */
    public GitRepository importRepository(String folderName) throws RepositoryException {
        if (folderName == null || folderName.trim().isEmpty()) {
            return null;
        }

        try {
            // 检查物理仓库是否存在（尝试带.git和不带.git的路径）
            File repoDir = new File(repositoriesBasePath, folderName);
            if (!repoDir.exists())
                return null;

            // 提取仓库名（保持与网页显示逻辑一致）
            String mapping = folderName;
            if (mapping.endsWith(".git")) {
                mapping = mapping.substring(0, mapping.length() - 4);
            }

            // 检查映射是否已存在
            List<GitRepository> existing = dbManager.getRepositoryDao()
                    .queryForEq("mapping", mapping);
            if (!existing.isEmpty()) {
                return null; // 已经在数据库中
            }

            // 简单的Git仓库验证
            boolean isGit = false;
            // 裸仓库：根目录下有 HEAD 和 config
            if (new File(repoDir, "HEAD").exists() && new File(repoDir, "config").exists()) {
                isGit = true;
            }
            // 非裸仓库：子目录下有 .git
            else if (new File(repoDir, ".git").exists()) {
                isGit = true;
            }

            if (isGit) {
                // 创建数据库记录
                // 注意：由于GitServer逻辑，mapping不带.git，前端显示会自动补齐
                GitRepository repository = new GitRepository(mapping, mapping, "");
                dbManager.getRepositoryDao().create(repository);
                Log.i(TAG, "Imported repository: " + mapping + " from " + folderName);
                return repository;
            }

            return null;

        } catch (SQLException e) {
            throw new RepositoryException("Database error while importing repository", e);
        }
    }

    /**
     * 扫描目录并导入所有发现的仓库
     */
    public int scanAndImportAll() {
        File dir = new File(repositoriesBasePath);
        if (!dir.exists() || !dir.isDirectory()) {
            Log.w(TAG, "Scan root directory does not exist: " + repositoriesBasePath);
            return 0;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            Log.e(TAG, "Failed to list files in " + repositoriesBasePath + " (Permission problem?)");
            return 0;
        }

        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                try {
                    if (importRepository(f.getName()) != null) {
                        count++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to import " + f.getName(), e);
                }
            }
        }
        return count;
    }

    /**
     * 删除仓库
     * 
     * @param repositoryId 仓库ID
     */
    public void deleteRepository(int repositoryId) throws RepositoryException {
        try {
            GitRepository repository = dbManager.getRepositoryDao().queryForId(repositoryId);
            if (repository == null) {
                throw new RepositoryException("Repository not found: " + repositoryId);
            }

            // 删除物理文件
            File repoDir = getRepositoryPath(repository.getMapping());
            if (repoDir.exists()) {
                deleteDirectory(repoDir);
                Log.i(TAG, "Deleted repository files: " + repoDir.getAbsolutePath());
            }

            // 删除数据库记录
            dbManager.getRepositoryDao().deleteById(repositoryId);
            Log.i(TAG, "Deleted repository from database: " + repository.getName());

        } catch (SQLException e) {
            throw new RepositoryException("Database error while deleting repository", e);
        }
    }

    /**
     * 更新仓库信息
     */
    public void updateRepository(int repositoryId, String description)
            throws RepositoryException {
        try {
            GitRepository repository = dbManager.getRepositoryDao().queryForId(repositoryId);
            if (repository == null) {
                throw new RepositoryException("Repository not found: " + repositoryId);
            }

            repository.setDescription(description);
            dbManager.getRepositoryDao().update(repository);

            Log.i(TAG, "Updated repository: " + repository.getName());

        } catch (SQLException e) {
            throw new RepositoryException("Database error while updating repository", e);
        }
    }

    /**
     * 更新仓库对象的所有字段
     * 
     * @param repository 仓库对象
     */
    public void updateRepository(GitRepository repository) throws RepositoryException {
        try {
            dbManager.getRepositoryDao().update(repository);
            Log.i(TAG, "Updated repository: " + repository.getName());
        } catch (SQLException e) {
            throw new RepositoryException("Database error while updating repository", e);
        }
    }

    /**
     * 获取所有仓库
     */
    public List<GitRepository> getAllRepositories() throws RepositoryException {
        try {
            return dbManager.getRepositoryDao().queryForAll();
        } catch (SQLException e) {
            throw new RepositoryException("Database error while fetching repositories", e);
        }
    }

    /**
     * 根据mapping查找仓库
     */
    public GitRepository getRepositoryByMapping(String mapping) throws RepositoryException {
        try {
            List<GitRepository> repos = dbManager.getRepositoryDao()
                    .queryForEq("mapping", mapping);
            return repos.isEmpty() ? null : repos.get(0);
        } catch (SQLException e) {
            throw new RepositoryException("Database error while fetching repository", e);
        }
    }

    /**
     * 获取仓库的物理路径
     */
    public File getRepositoryPath(String mapping) {
        String dirName = mapping.endsWith(".git") ? mapping : mapping + ".git";
        return new File(repositoriesBasePath, dirName);
    }

    /**
     * 打开JGit仓库对象
     */
    public org.eclipse.jgit.lib.Repository openJGitRepository(String mapping)
            throws RepositoryException {
        try {
            File repoDir = getRepositoryPath(mapping);
            if (!repoDir.exists()) {
                throw new RepositoryException("Repository not found: " + mapping);
            }

            org.eclipse.jgit.storage.file.FileRepositoryBuilder builder = new org.eclipse.jgit.storage.file.FileRepositoryBuilder();
            return builder.setGitDir(repoDir)
                    .readEnvironment()
                    .findGitDir()
                    .build();

        } catch (IOException e) {
            throw new RepositoryException("Failed to open repository: " + mapping, e);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    /**
     * 释放资源
     */
    public void close() {
    }

    /**
     * 仓库操作异常
     */
    public static class RepositoryException extends Exception {
        public RepositoryException(String message) {
            super(message);
        }

        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
