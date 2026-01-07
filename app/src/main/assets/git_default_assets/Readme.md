# Git Default Assets

## 概述 / Overview

此目录包含DroidGit应用的默认Git资源文件。

This directory contains default Git asset files for the DroidGit application.

## 文件列表 / File List

### git_repo_retarget.bat
Git仓库地址重定向批处理脚本

Batch script for redirecting Git repository addresses

#### 功能 / Features

该脚本用于将本地Git仓库的所有远程地址批量更改为新的仓库地址。

This script is used to batch change all remote addresses of a local Git repository to a new repository address.

#### 主要功能 / Main Functions

1. **分支同步** / Branch Synchronization
   - 自动检出所有远程分支到本地
   - Auto-checkout all remote branches to local

2. **远程地址更新** / Remote URL Update
   - 批量更新所有远程仓库地址
   - Batch update all remote repository addresses

3. **数据推送** / Data Push
   - 镜像推送所有分支和标签
   - Mirror push all branches and tags

4. **清理同步** / Cleanup Sync
   - 获取最新远程信息并清理已删除的引用
   - Fetch latest remote info and clean up deleted references

#### 使用方法 / Usage

1. 将此脚本复制到目标Git仓库的根目录
   Copy this script to the root directory of the target Git repository

2. 在命令提示符中运行脚本
   Run the script in Command Prompt

3. 脚本会显示当前的远程地址配置
   The script will display current remote address configuration

4. 输入新的Git仓库完整URL
   Enter the new Git repository full URL

5. 脚本自动完成以下操作：
   The script automatically performs the following operations:
   - 更新所有远程地址
   - Update all remote addresses
   - 镜像推送所有数据
   - Mirror push all data
   - 同步获取最新信息
   - Sync fetch latest information
   - 强制推送所有分支和标签
   - Force push all branches and tags

#### 注意事项 / Notes

⚠️ **重要警告** / **Important Warning**
- 此操作会强制覆盖远程仓库的数据
- This operation will force overwrite remote repository data
- 请确保有完整的备份后再执行
- Please ensure you have complete backups before execution
- 建议在测试环境中先验证脚本功能
- It's recommended to verify script functionality in a test environment first

#### 使用示例 / Usage Example

```batch
# 运行脚本
> git_repo_retarget.bat

# 输出示例
==============================
Current Git remote addresses:
==============================
origin  https://10.0.0.100/old-user/old-repo.git (fetch)
origin  https://10.0.0.100/old-user/old-repo.git (push)

Please enter the new Git repository address (full URL):
> https://10.0.0.100/new-user/new-repo.git

Updating all remote addresses (applies to all branches)...
Modifying remote: origin

==============================
Modification completed (all branches now use the new address):
==============================
origin  https://10.0.0.100/new-user/new-repo.git (fetch)
origin  https://10.0.0.100/new-user/new-repo.git (push)
```

#### 适用场景 / Applicable Scenarios

- 仓库迁移 / Repository migration
- 更换托管平台 / Changing hosting platform
- 统一远程地址 / Unifying remote addresses
- 团队协作地址更新 / Team collaboration address update

---