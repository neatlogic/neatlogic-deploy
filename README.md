中文 / [English](README.en.md)

## 关于

neatlogic-deploy是发布模块，可以解决应用环境一键发布的场景，主要包括应用配置、版本中心、一键发布等功能。

## 主要功能

### 应用配置

应用配置主要是管理应用、模块层和环境层的流水线脚本和参数等配置。
![img.png](README_IMAGES/img1.png)
![img.png](README_IMAGES/img.png)
- 支持配置应用持续集成
- 支持配置应用超级流水线，模板中只能选当前应用。
- 支持配置通知策略
- 支持通过授权管理使用者范围
- 支持编辑应用、模块和环境信息

### 版本中心

版本中心是用于管理应用模块的版本及版本的工程物料。已编译过的版本的工程物理库支持上传和下载
![img.png](README_IMAGES/img2.png)
![img.png](README_IMAGES/img3.png)

### 一键发布

一键发布页面支持发起单个发布作业和发起批量作业
1. 发起单个发布作业<br>
选择发起作业的应用、模块（已配置流水线和环境），然后点击添加作业按钮，完成发起发布作业配置并保存
![img.png](README_IMAGES/img4.png)
![img.png](README_IMAGES/img5.png)
2. 批量发布作业<br>
批量发布作业可以直接创建也可以通过超级流水线发起。直接创建是将当前已有作业添加到一个集合里，超级流水线方式是通过模板发起。
![img.png](README_IMAGES/img6.png)
![img.png](README_IMAGES/img7.png)
![img.png](README_IMAGES/img8.png)

### 超级流水线

超级流水线是管理全局批量发布作业模板，超级流水线支持发起批量作业和发起定时作业。
![img.png](README_IMAGES/img9.png)
![img.png](README_IMAGES/img10.png)

### 定时作业
定时作业通过配置固定作业定时器实现定时发起发布作业，发起的作业类型支持普通作业和超级流水线两种。
![img.png](README_IMAGES/img11.png)

 ### Webhook
 webhook页面是管理作业触发器，包括配置触发范围和触发动作，触发范围即触发点，触发范围的对象是应用模块的环境关联的作业状态，触发动作则是关联“发布触发器数据规范”类型的集成配置。
 ![img.png](README_IMAGES/img12.png)