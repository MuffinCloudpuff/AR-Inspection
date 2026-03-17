<<<<<<< HEAD
# AR-Inspection
基于雷鸟X2的AR巡检
=======
# AR 智能巡检系统 (RayNeo AR SDK + Django)

## 📌 项目定位
这是一个 **基于雷鸟 (RayNeo) AR 眼镜 + Android 客户端 + Django 后端** 的工业级巡检系统。
该系统旨在通过 AR 硬件适配与闭环的任务管理流程，提升巡检人员的工作效率，确保巡检记录的真实性与结构化。

## 🚀 核心功能
- **任务全闭环管理**：从后端创建任务、眼镜端拉取任务、执行巡检、到最后提交任务，形成完整闭环。
- **AR 实景适配**：针对雷鸟 AR 眼镜的双目显示特性，重构传统手机 UI 为双区域显示（预览区 + 状态区）。
- **智能位置识别**：通过 CameraX 接入扫码功能，扫描巡检点二维码自动获取位置标识。
- **结构化数据采集**：图片、语音上传时自动与特定巡检位置绑定，避免记录混乱。
- **安全通讯**：集成 CSRF 令牌校验，确保移动端与 Django 后端的通讯安全。

## 🛠 技术路线
### 前端 (Android 客户端)
- **SDK**: 基于官方 RayNeo AR SDK 二次开发。
- **相机**: 使用 CameraX 框架处理预览与扫码，针对眼镜端 16:9 长宽比及方向旋转（90° 适配）进行优化。
- **交互**: 接入 `TempleActionViewModel` 与焦点追踪模型，适配眼镜触控板手势。
- **网络**: 通过 Retrofit2 / OkHttp 与后端进行 RESTful API 通讯。

### 后端 (Django 服务端)
- **框架**: Django 5.1 + Django REST Framework。
- **模块**:
    - `mission`: 维护任务状态流转（未完成、进行中、已完成）。
    - `photo`: 管理巡检照片，支持按位置标识聚合。
    - `audiorecord`: 管理巡检语音备注。
    - `getcsrftoken`: 提供跨端安全校验令牌。
- **管理端**: 基于 Django Admin + AdminLTE，提供结构化的巡检结果展示。

## 💎 项目亮点与难点
1. **特定硬件适配**：克服了 AR 眼镜与普通安卓在显示范式和交互逻辑上的差异，实现了符合眼镜使用习惯的双目 UI 布局。
2. **位置与数据强绑定**：解决了巡检过程中同一位置多张图、补拍、乱序等导致的记录混乱问题，确保报告生成时的准确性。
3. **局域网联调优化**：处理了 Django `ALLOWED_HOSTS` 配置、局域网监听地址绑定及移动端跨域校验等真实场景问题。

## 📂 项目结构
```text
.
├── RayNeoARSDKAndroidDemo   # Android 客户端工程
│   └── app/src/main         # 核心代码与 AR SDK 适配逻辑
├── django-project-master    # Django 后端工程
│   ├── mission/             # 任务管理模块
│   ├── photo/               # 图片管理模块
│   └── media/               # 媒体文件存储
└── README.md
```

---
*本项目基于官方 RayNeo AR SDK Demo 进行深度的二次开发与业务重构。*
>>>>>>> 75ce997 (docs: init project with comprehensive README and .gitignore)
