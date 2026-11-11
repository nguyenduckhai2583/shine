# AGENTS.md — Shine Android Project Guidelines

Tài liệu này cung cấp toàn bộ hướng dẫn, quy chuẩn kiến trúc, công nghệ và các quy tắc phát triển dành cho các AI Agent và Developer khi làm việc trên dự án **Shine**.

---

## 1. Tổng quan Dự án (Project Overview)

- **Tên dự án:** Shine
- **Nền tảng:** Android (Native)
- **Package name:** `com.example.shine`
- **Mục tiêu:** Ứng dụng điều phối công việc, giao tiếp doanh nghiệp (Workspace, Channels, Direct Messages, Project/Planix Management, Profile).
- **API Server:** `https://employer-api.dev.hodfords.uk/`

---

## 2. Công nghệ & Thư viện Chính (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Phiên bản / Ghi chú |
| :--- | :--- | :--- |
| **Language & SDK** | Kotlin, Java 17 | Kotlin `2.4.20`, compileSdk/targetSdk `37`, minSdk `24` |
| **Build System** | Gradle Version Catalogs (`libs.versions.toml`), AGP | AGP `9.4.1`, KSP `2.3.12` |
| **UI Framework** | Jetpack Compose + Material 3 | Compose BOM `2026.09.00` |
| **Navigation** | **AndroidX Navigation 3** (mới nhất) | `androidx.navigation3:navigation3-runtime:1.2.0`, `navigation3-ui` |
| **Dependency Injection** | Dagger Hilt | `hilt-android:2.60.1`, Hilt Navigation Compose |
| **Networking** | Retrofit 3 + OkHttp 5 | `retrofit:3.0.0`, `okhttp:5.5.0`, Kotlinx Serialization Json |
| **Local Storage** | Room Database + DataStore Preferences | Room `2.8.5`, DataStore `1.2.1` |
| **Paging** | AndroidX Paging 3 | Paging Compose `3.3.6`, `RemoteMediator`, `PagingSource` |
| **Image Loading** | Coil Compose | `io.coil-kt:coil-compose:2.7.0` |
| **Testing** | JUnit 4, Kotlinx Coroutines Test, Mockito, MockWebServer, Paging Testing | Hỗ trợ Unit Test toàn diện cho ViewModel & Data layers |

---

## 3. Cấu trúc Thư mục (Project Structure)

Dự án đang áp dụng **Multi-module Architecture (theo chuẩn Google Now in Android)** kết hợp **Clean Architecture** & UDF:

```
Shine/
├── core/
│   ├── model/                     # Pure data models (Channel, User, Workspace, Project, Session, SyncStatus, SignInStatus)
│   ├── common/                    # Core utilities, Dispatchers, NetworkMonitor, AppLifecycleMonitor, AppException, UiText
│   ├── database/                  # Room Database (ShineDatabase), Entities, DAOs, DatabaseModule
│   ├── datastore/                 # DataStore Preferences (Session, Tokens, Workspace, DMs), DataStoreModule
│   ├── network/                   # Retrofit APIs, DTOs & Mappers, OkHttp Interceptors, Socket.IO engine, NetworkModule
│   └── data/                      # Repository (interfaces & impls), Paging (PlanixRemoteMediator), Sync (Worker & Scheduler), DataModule
└── app/
    ├── src/main/java/com/example/shine/
    │   ├── ShineApplication.kt    # Application class, @HiltAndroidApp
    │   ├── MainActivity.kt        # Single Activity, Compose & Deep Links
    │   ├── core/                  # App-level di (ShortcutModule, WidgetSyncListenerModule) & shortcuts
    │   ├── ui/                    # Feature screens (home, channel, dms, planix, profile, signin, workspace, navigation)
    │   └── widget/                # Glance AppWidget & sync managers
```

---

## 4. Quy tắc Thiết kế & Kiến trúc (Architectural Guidelines)

### 4.1. Modern Android Architecture & Unidirectional Data Flow (UDF)
- **Tầng Data (:core:data):** Đóng vai trò là *Single Source of Truth* cho toàn bộ ứng dụng. Cung cấp các Repository interfaces (Public API) cho các tầng UI/Feature tiêu thụ, và đóng gói chi tiết triển khai (Room, Network, DataStore, Paging 3, Sync Worker).
- **Tầng UI:** 
  - Chỉ quan sát trạng thái qua `StateFlow<UiState>` từ `ViewModel`.
  - Mọi sự kiện từ người dùng (User Actions) gửi tới ViewModel thông qua các public method.
  - Sử dụng Stateless Composable cho UI rendering và Stateful Composable (Route) để liên kết ViewModel.

### 4.2. Quản lý Điều hướng (Jetpack Navigation 3)
> [!IMPORTANT]
> Dự án sử dụng **Jetpack Navigation 3** (không dùng NavHostController / Navigation 2 XML / chuỗi String route cũ).

- **Khai báo NavKey:** Đặt trong `ui/navigation/NavKeys.kt`, kế thừa `androidx.navigation3.runtime.NavKey` và có annotation `@Serializable`.
  ```kotlin
  @Serializable
  data object Home : NavKey

  @Serializable
  data class ChannelDetail(val channelId: String) : NavKey
  ```
- **Quản lý BackStack:** Sử dụng `rememberNavBackStack(...)`, `NavDisplay(...)`, phối hợp với decorators:
  - `rememberSaveableStateHolderNavEntryDecorator()`
  - `rememberViewModelStoreNavEntryDecorator()`
- **Phân tách luồng xác thực:** Trong `ShineApp.kt`, back stack của trạng thái `SignedOut` (SignIn), `SelectWorkspace`, và `SignedIn` (Home) được tách biệt hoàn toàn để tránh rò rỉ trạng thái giữa các phiên đăng nhập.

### 4.3. Quản lý Phiên (Session) & Xác thực
- **SessionLocalDataSource:** Quản lý `Session` (Token, RefreshToken, User, WorkspaceId) lưu trữ bảo mật trong DataStore.
- **Tự động làm mới Token:** 
  - `AuthInterceptor` thêm header `Authorization: Bearer <token>` vào request.
  - `TokenRefreshScheduler` / `TokenRefresher` quản lý chu kỳ làm mới token trước khi hết hạn.

### 4.4. Xử lý Lỗi (Error Handling)
- **Tập trung hóa AppException:** Tất cả lỗi mạng, máy chủ và không có Internet đều được chuyển đổi thành `AppException(val error: AppError, val serverMessage: String? = null)`.
- **ErrorMappingCallAdapterFactory:** Retrofit CallAdapter tự động bọc response lỗi HTTP và IO thành `AppException`.
- **Hiển thị thông báo trên UI:** Sử dụng `UiText` (hỗ trợ cả String tĩnh lẫn String Resource ID) hoặc gán thông điệp lỗi vào `errorMessage` của `UiState`.

### 4.5. Paging 3 & Caching
- Danh sách lớn (ví dụ: Planix list) sử dụng Room kết hợp **Paging 3** (`RemoteMediator` hoặc `PagingSource`) để hỗ trợ load phân trang vô tận và cache offline.

---

## 5. Quy chuẩn Viết Code & Phong cách (Coding Conventions)

1. **Immutability:**
   - UI State phải luôn là `data class` với các thuộc tính `val`.
   - Cập nhật state bằng `_uiState.update { current -> current.copy(...) }`.
2. **Dependency Injection:**
   - Sử dụng Hilt: Thêm `@HiltViewModel` cho tất cả ViewModels, inject repository thông qua interface thay vì class implementation cụ thể.
   - Dispatchers cho Coroutines cần được inject thông qua `@IoDispatcher`, `@DefaultDispatcher` từ `CoroutinesModule` để thuận tiện viết Unit Test.
3. **Coroutines & Scopes:**
   - Trong ViewModel, chỉ launch coroutines trong `viewModelScope`.
   - Thu thập Flow trên UI Compose bằng `collectAsStateWithLifecycle()`.
4. **Bảo tồn Comment & Tài liệu:** Giữ nguyên các comment giải thích nghiệp vụ quan trọng trong code hiện có.

---

## 6. Quy tắc Kiểm thử (Testing Standards)

> [!IMPORTANT]
> Dự án duy trì độ phủ Unit Test rất cao. Mọi tính năng mới hoặc thay đổi logic trong ViewModel, Repository, DataSource, Interceptor đều **bắt buộc** phải có Unit Test đi kèm trong `app/src/test`.

### 6.1. Quy chuẩn Unit Test cho ViewModel
- Sử dụng **Fake Repository Pattern** (triển khai Interface từ domain) thay vì lạm dụng mockito nếu không cần thiết.
- Thiết lập Coroutine Test Dispatcher bằng `UnconfinedTestDispatcher()` trong `@Before` và `@After`:
  ```kotlin
  @Before
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @After
  fun tearDown() = Dispatchers.resetMain()
  ```
- Kích hoạt thu thập StateFlow trong background scope để kiểm tra emission:
  ```kotlin
  backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect {}
  }
  ```

### 6.2. Kiểm thử Tầng Dữ liệu (Data Layer Testing)
- Sử dụng `MockWebServer` để kiểm tra Retrofit APIs, Interceptors, và CallAdapters.
- Sử dụng In-Memory Database (`Room.inMemoryDatabaseBuilder`) hoặc Mock DAO khi kiểm thử Repository và Mediator.

---

## 7. Các Lệnh Thường dùng (Common Commands)

```bash
# Kiểm tra build dự án
./gradlew assembleDebug

# Chạy toàn bộ Unit Tests
./gradlew testDebugUnitTest

# Chạy một lớp test cụ thể
./gradlew testDebugUnitTest --tests "com.example.shine.ui.profile.ProfileViewModelTest"

# Kiểm tra lint
./gradlew lintDebug
```
