# AR巡检项目梳理（精简版）

## 一、项目一句话概括

这是一个 **基于雷鸟 AR 眼镜 + Android + Django** 的巡检系统，我主要做的是眼镜端二次开发、前后端联通和巡检流程设计，最终把“任务下发 - 扫码识别位置 - 拍照上传 - 提交任务 - 按位置整理结果”这条链路打通了。

---

## 二、项目架构

### 1）前端 / 终端
- 基于官方 `RayNeo AR SDK Demo` 二次开发；
- 安卓端不是普通手机界面，而是针对 AR 眼镜重新做了显示和交互适配；
- 接入 CameraX 做相机预览 / 扫码场景支持；
- 通过 RayNeo SDK 的焦点和动作事件体系处理操作。

### 2）后端
- 使用 Django 搭建；
- 模块包括：`mission`（任务）、`photo`（图片）、`audiorecord`（语音）、`getcsrftoken`；
- 后端负责任务管理、图片上传、语音上传、任务状态维护和管理页面展示。

### 3）核心数据思路
- 任务：`task + name + state`
- 图片：`name + time_code + image_file`
- 其中图片上传时的 `name` 不只是普通名字，在实际业务里用来承载 **巡检位置标识**。

---

## 三、最关键的业务流程

1. 后端创建巡检任务；
2. 眼镜端连接后端，按执行人拉取任务；
3. 到达巡检点后扫码，二维码中带有位置内容，比如“咖啡机”；
4. 拍照上传时，把 **图片和位置一起上传**；
5. 多个点位重复执行；
6. 巡检完成后手动提交任务；
7. 后端把任务改为完成状态，并按相同位置整理对应图片，形成巡检结果。

---

## 四、最值得讲的难点与解决方案

## 难点 1：AR眼镜不是普通安卓设备
### 问题
- 眼镜是双目显示，交互方式也不是普通手机点击；
- 直接照搬安卓页面和交互逻辑，效果不对，也不好用。

### 解决方案
- 基于官方 RayNeo Demo 二次开发；
- 按眼镜端场景重构页面，把界面拆成适合眼镜观看的双区域；
- 按 SDK 的焦点和动作模型来处理交互，而不是只写点击事件。

### 可讲亮点
- 做过特定硬件平台适配，不是普通 Android 页面开发。

---

## 难点 2：相机 / 扫码适配
### 问题
- 眼镜端相机和手机不同，预览方向、视野方向都需要重新适配；
- 你实际还遇到过需要旋转 90° 后视野才正常的问题。

### 解决方案
- 接入 CameraX；
- 在横屏场景下重新适配预览逻辑；
- 处理扫码画面方向和显示方向问题，保证位置识别稳定。

---

## 难点 3：前后端联通
### 问题
- 眼镜端和 Django 后端在同一局域网里，最开始仍然连不上；
- 不只是接口问题，还涉及 Django 对外访问配置和请求校验。

### 解决方案
- 服务监听地址改为局域网可访问；
- 调整 `ALLOWED_HOSTS`；
- 使用真实局域网 IP 联调；
- 处理 Django 的 CSRF token 请求。

### 说明
- 现存代码里能直接看到 `ALLOWED_HOSTS = []` 和获取 CSRF token 的接口；
- 所以这一块确实是你当时联调的重点。

---

## 难点 4：图片和巡检位置的绑定，这是项目里最重要的设计点
### 问题
- 如果只是扫码后直接拍照上传，同一个位置拍多张图、补拍、换角度拍，最后报告一定会乱；
- 后端只会拿到一堆图片，却不知道每张图对应哪个巡检点。

### 解决方案
- 把巡检位置直接写进二维码；
- 扫码先获取位置；
- 上传图片时，把 **图片和位置信息一起上传**；
- 后端最终按位置聚合图片，而不是按上传顺序拼结果。

### 结果
- 同一个位置拍多张图也不会乱；
- 任务结束后能自动按位置整理图片。

---

## 难点 5：任务流必须闭环
### 问题
- 巡检系统不能只做到上传，还必须知道任务什么时候真正完成；
- 没有结束机制，就不能正确产出结果。

### 解决方案
- 设计成完整流程：任务下发 → 巡检执行 → 上传记录 → 提交任务 → 状态改为已完成 → 整理结果；
- 通过任务状态字段维护“未完成 / 进行中 / 已完成”。

---

## 五、面试时最推荐的总结说法

> 这个项目我主要做的是基于 RayNeo AR 眼镜的安卓端二次开发和 Django 后端联调。难点主要有三个：第一是 AR 眼镜和普通安卓在显示、交互和相机适配上完全不同；第二是局域网环境下前后端联通和请求校验比较麻烦；第三也是最关键的一点，是巡检图片和位置的绑定问题。我最终通过“二维码携带位置信息，上传时图片与位置一起入库”的方式，把整个任务流做成了闭环，任务结束后系统可以按位置自动整理多张巡检图片。

---

## 六、代码依据
- Android 启动页已从官方 `DemoHomeActivity` 改成 `MainActivity`：`ar巡检/RayNeoARSDKAndroidDemo20250227_1/RayNeoARSDKAndroidDemo/app/src/main/AndroidManifest.xml:25`
- 新增相机页：`ar巡检/RayNeoARSDKAndroidDemo20250227_1/RayNeoARSDKAndroidDemo/app/src/main/java/com/rayneo/arsdk/android/demo/ui/activity/CameraPreviewActivity.kt:19`
- 眼镜端双区域布局：`ar巡检/RayNeoARSDKAndroidDemo20250227_1/RayNeoARSDKAndroidDemo/app/src/main/res/layout/activity_camera_preview.xml:8`
- RayNeo SDK 交互事件：`ar巡检/RayNeoARSDKAndroidDemo20250227_1/RayNeoARSDKAndroidDemo/app/src/main/java/com/rayneo/arsdk/android/demo/ui/wedget/TitleView.kt:134`
- Django `ALLOWED_HOSTS`：`ar巡检/django-project-master/djangoProject/settings.py:30`
- 按人查任务接口：`ar巡检/django-project-master/mission/views.py:91`
- 图片上传接口：`ar巡检/django-project-master/photo/views.py:36`
- CSRF token 接口：`ar巡检/django-project-master/getcsrftoken/views.py:11`
