# 开源依赖决策

| 依赖 | 坐标/版本 | 决策 | 理由 | 隔离层 |
|---|---|---|---|---|
| Mirage | io.github.androidpoet:mirage:0.1.0 | 采用 | 零传递依赖、Compose 原生、AGSL 纸张纹理、API 33 以下可降级；在本工程实测编译通过 | `ui/visual/PaperTexture.kt` |
| Paper Shaders Android | source module 0.0.3 | 不采用 | 与 Mirage 功能重叠，需引入源码模块，维护成本高 | — |
| Lottie Android | com.airbnb.android:lottie 6.7.1 | 暂缓 | 无合规动画资产，不为空依赖增加包体积 | — |
| dotLottie Android | com.lottiefiles:dotlottie-android 0.18.1 | 不采用 | Rust/NDK 内核增加集成复杂度，且无动画资产 | — |
| Compottie | io.github.alexzhirkevich:compottie | 不采用 | 面向 Compose Multiplatform，本工程 Android-only | — |
| open-collage | CC BY 4.0 | 不采用 | 素材未能逐项核验授权与透明度，禁止下载即使用 | — |
