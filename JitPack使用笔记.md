## JitPack 使用笔记：轻松将 GitHub 项目转化为 Maven/Gradle 依赖

---



### 1. 我遇到的问题：想用未发布到 Maven 仓库的 GitHub 项目

在我开发一个 Spring Boot 项目时，我希望集成百度的 UidGenerator (https://github.com/baidu/uid-generator)，并计划将其封装成一个 Spring Boot Starter 以便复用。

然而，我很快发现 `uid-generator` 项目并没有发布到 Maven 中央仓库 (Maven Central)。此外，该项目的 GitHub 仓库也没有提供正式的 "Releases" (标签版本)，这意味着我无法像通常那样通过一个稳定的版本号 (如 `1.0.0`) 来引入这个依赖。

**这给我带来了困扰：**

- **无法直接在 `pom.xml` 中声明依赖：** 因为它不在中央仓库。
- **手动管理不便：** 下载源码、手动构建并安装到本地 Maven 仓库 (`.m2`) 或者将源码作为项目模块引入，这些方法虽然可行，但在团队协作、CI/CD 环境下以及后续版本更新时都显得不够便捷和规范。

我需要一个简单的方法，能够像使用普通 Maven 依赖一样来使用这个 GitHub 上的项目。

---



### 2. JitPack 如何帮我解决问题

通过你的建议，我了解并尝试了 JitPack。JitPack 完美地解决了我的问题：

1. **直接从 GitHub 构建：** JitPack 可以直接从 GitHub (或其他 Git 仓库) 拉取项目源码。
2. **使用 Commit Hash 作为版本：** 即使 `uid-generator` 没有发布正式的 Release 版本，JitPack 允许我使用项目中的任意一个 **Commit Hash** (提交哈希值) 作为版本号。这让我能够锁定到一个特定的代码快照，确保了依赖的稳定性。
3. **引入成功：** 我在我的 `pom.xml` 中添加了 JitPack 仓库，并按照 JitPack 的规则配置了 `uid-generator` 的依赖 (groupId 为 `com.github.baidu`，artifactId 为 `uid-generator`，version 为我选定的一个 Commit Hash)。Maven 成功地从 JitPack 下载并引入了这个依赖。

**具体步骤如下 (以 Maven 为例)：**

- 添加 JitPack 仓库到 `pom.xml`：

  XML

  ```xml
  <repositories>
      <repository>
          <id>jitpack.io</id>
          <url>https://jitpack.io</url>
      </repository>
  </repositories>
  ```

- 添加 `uid-generator` 依赖，使用 Commit Hash 作为版本：

  XML

  ```xml
  <dependency>
      <groupId>com.github.baidu</groupId> 
      <artifactId>uid-generator</artifactId>
      <version>YOUR_CHOSEN_COMMIT_HASH</version> 
  </dependency>
  ```

就这样，`uid-generator` 就被成功引入到我的项目中了，我可以继续进行我的 Spring Boot Starter 开发工作！

---



### 3. JitPack 是什么？怎么使用？

#### 3.1 JitPack 是什么？

JitPack 是一个便捷的 **按需打包和发布的 Maven/Gradle 仓库服务**。它能够直接从 **GitHub、Bitbucket、GitLab、Gitee** 等 Git 托管服务上拉取项目源码，并动态地将其构建成一个可用的 Java 库 (jar, aar 等)。

**核心优势：**

- **无需项目所有者发布：** 即使项目作者没有将其发布到 Maven Central 或其他公共仓库，你也可以通过 JitPack 使用它。
- **灵活的版本控制：** 你可以使用项目的 Release 标签、Commit Hash、分支名，甚至是 Pull Request ID 作为版本号。
- **简单易用：** 只需简单配置即可开始使用。
- **公开免费：** 对于公开的 GitHub 仓库，JitPack 的服务是免费的。它也提供针对私有仓库的付费服务。

#### 3.2 如何使用 JitPack

##### 3.2.1 对于 Maven 项目

1. 添加 JitPack 仓库：

   在你的项目根 pom.xml 文件 (或者需要该依赖的模块的 pom.xml) 中的 <repositories> 部分添加 JitPack 仓库：

   XML

   ```xml
   <repositories>
       <repository>
           <id>jitpack.io</id>
           <url>https://jitpack.io</url>
       </repository>
   </repositories>
   ```

2. 添加依赖：

   在 <dependencies> 部分添加你想要的 GitHub 项目作为依赖。依赖的坐标格式如下：

   - **GroupId:** 对于 GitHub 项目，通常是 `com.github.用户名` 或 `com.github.组织名`。例如，如果项目地址是 `https://github.com/UserA/RepoX`，则 groupId 是 `com.github.UserA`。
   - **ArtifactId:** GitHub 仓库的名称。例如，`RepoX`。
   - Version:
     - **Release Tag:** 如果项目有 GitHub Releases (例如 `v1.0.2`)，直接使用该标签名：`1.0.2`。
     - **Commit Hash:** 任意一个 commit 的完整或简短 SHA hash：`0f8f7b93a2` 或 `0f8f7b93a25b8b00f0a9c2ea1b57d3d64398e109`。这是我这次使用的方法。
     - **Branch Name:** 分支名后加 `-SNAPSHOT`。例如，使用 `master` 分支的最新提交：`master-SNAPSHOT`。使用 `develop` 分支：`develop-SNAPSHOT`。
     - **Pull Request (不常用作稳定依赖):** `PR-PULL_REQUEST_NUMBER` (例如 `PR-123`) 可以构建某个 Pull Request。
     - **`LATEST` (不推荐用于生产):** 可以使用 `LATEST` 关键字，它会尝试构建最新的 Release Tag，如果没有 Release Tag，则会构建 `master` 分支的最新 commit。这可能导致构建不稳定。

   **示例：**

   XML

   ```xml
   <dependency>
       <groupId>com.github.UserA</groupId>
       <artifactId>RepoX</artifactId>
       <version>v1.0.2</version> 
   </dependency>
   
   <dependency>
       <groupId>com.github.AnotherUser</groupId>
       <artifactId>SomeLibrary</artifactId>
       <version>a1b2c3d4e5</version>
   </dependency>
   
   <dependency>
       <groupId>com.github.OrgY</groupId>
       <artifactId>ProjectZ</artifactId>
       <version>develop-SNAPSHOT</version> 
   </dependency>
   ```

##### 3.2.2 对于 Gradle 项目

1. 添加 JitPack 仓库：

   在你的项目根 build.gradle (或 build.gradle.kts) 文件的 repositories 闭包中添加 JitPack：

   - Groovy DSL (`build.gradle`):

     

     Groovy

     ```
     allprojects {
         repositories {
             ...
             maven { url 'https://jitpack.io' }
         }
     }
     ```

     

   - 

   - Kotlin DSL (`build.gradle.kts`):

     

     Kotlin

     ```
     allprojects {
         repositories {
             ...
             maven("https://jitpack.io")
         }
     }
     ```

   或者，如果你只在特定模块中使用，可以添加到该模块的 `build.gradle` 中。

2. 添加依赖：

   在你的模块 build.gradle (或 build.gradle.kts) 文件的 dependencies 闭包中添加依赖，格式与 Maven 类似：'com.github.User:Repo:Version'。

   - Groovy DSL (`build.gradle`):

     Groovy

     ```
     dependencies {
         implementation 'com.github.UserA:RepoX:v1.0.2' // Release Tag
         implementation 'com.github.AnotherUser:SomeLibrary:a1b2c3d4e5' // Commit Hash
         implementation 'com.github.OrgY:ProjectZ:develop-SNAPSHOT' // Branch
     }
     ```

   - Kotlin DSL (`build.gradle.kts`):

     Kotlin

     ```
     dependencies {
         implementation("com.github.UserA:RepoX:v1.0.2")
         implementation("com.github.AnotherUser:SomeLibrary:a1b2c3d4e5")
         implementation("com.github.OrgY:ProjectZ:develop-SNAPSHOT")
     }
     ```

##### 3.2.3 查看构建状态和可用版本

你可以访问 [jitpack.io](https://jitpack.io/) 网站。

- **首页输入框：** 在首页输入 GitHub 仓库的 URL (例如 `github.com/baidu/uid-generator`) 或 `用户名/仓库名` (例如 `baidu/uid-generator`)。
- **查看构建日志：** JitPack 会显示该项目的构建状态。如果某个版本（tag、commit）是你第一次请求，JitPack 会在后台进行构建。你可以看到构建日志，如果构建失败，日志中通常会包含原因。
- **获取版本号：** 页面上会列出可用的版本（通常是 Release Tags 和最近的 Commits）。

#### 3.3 注意事项

- **构建时间：** 当你第一次请求某个项目/版本的依赖时，JitPack 需要克隆源码并执行构建（通常是 `mvn install` 或 `gradle build`）。这可能需要几分钟时间。一旦构建完成并缓存后，后续的下载会很快。
- **项目必须能被构建：** JitPack 依赖于项目本身包含有效的 `pom.xml` (Maven) 或 `build.gradle` (Gradle) 文件，并且能够成功构建。如果项目构建配置有问题，JitPack 也无法成功提供依赖。
- **SNAPSHOT 版本的缓存：** Maven 和 Gradle 都会缓存 `-SNAPSHOT` 版本的依赖。如果你使用的是分支的 `-SNAPSHOT` 版本，并且希望获取最新的提交，你可能需要强制更新依赖（例如，Maven 使用 `mvn clean install -U`，Gradle 在依赖声明时使用 `changing = true` 或命令行使用 `--refresh-dependencies`）。
- 稳定性：
  - 使用 **Release Tag** 是最稳定的，因为它代表了项目作者认证的一个稳定版本。
  - 使用 **Commit Hash** 也是比较稳定的，因为它锁定到代码的一个特定快照。
  - 使用 **Branch-SNAPSHOT** 版本（如 `master-SNAPSHOT`）则意味着你的依赖可能会随着该分支的更新而改变，这可能引入不兼容的更改，需要谨慎使用，尤其是在生产环境中。
- **依赖 JitPack 服务：** 你的构建过程会依赖于 JitPack 服务的可用性。虽然 JitPack 通常很稳定，但这仍是一个外部依赖点。

------

希望这份笔记对你有所帮助！JitPack 确实是一个解放生产力的好工具，特别是在处理那些尚未发布到传统仓库的开源项目时。