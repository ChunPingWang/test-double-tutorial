# Test Double 完整教學:以 Java / Spring 電商下訂單情境貫穿

本專案以一個**電商下訂單**的模擬案例,完整示範五種 Test Double(Dummy、Stub、Spy、Mock、Fake)的觀念與實作,並涵蓋 Mockito 與 Spring Boot 測試(`@WebMvcTest`、`@SpringBootTest`、`@MockitoBean`)的實務用法。

- 技術棧:Java 21、Spring Boot 3.4、JUnit 5、Mockito、AssertJ
- 所有測試可直接執行:`mvn test`(共 14 個測試)

---

## 目錄

1. [什麼是 Test Double?](#1-什麼是-test-double)
2. [為什麼需要 Test Double?](#2-為什麼需要-test-double)
3. [模擬案例:電商下訂單](#3-模擬案例電商下訂單)
4. [五種 Test Double 詳解](#4-五種-test-double-詳解)
5. [狀態驗證 vs 行為驗證](#5-狀態驗證-vs-行為驗證)
6. [Mockito 與五種分類的對照](#6-mockito-與五種分類的對照)
7. [Spring 測試中的 Test Double](#7-spring-測試中的-test-double)
8. [如何選擇:決策指南](#8-如何選擇決策指南)
9. [常見反模式](#9-常見反模式)
10. [專案結構與執行方式](#10-專案結構與執行方式)
11. [參考資料](#11-參考資料)

---

## 1. 什麼是 Test Double?

**Test Double(測試替身)** 一詞由 Gerard Meszaros 在《xUnit Test Patterns》中提出,靈感來自電影的**特技替身(stunt double)**:觀眾以為看到的是主角,實際上危險動作由替身完成。

在測試中也是一樣:**被測對象(SUT, System Under Test)以為自己在跟真實的協作者(資料庫、金流、郵件伺服器)互動,實際上面對的是我們為了測試而安插的替身**。

能夠安插替身的前提是「依賴反轉」:SUT 依賴**介面**而非具體實作,而實作由外部(建構子)注入——這正是 Spring 依賴注入(DI)天生擅長的事,也是 Spring 專案特別適合使用 test double 的原因。

```
                ┌──────────────────────┐
   測試程式 ──→ │  OrderService (SUT)  │
                └──────┬───────────────┘
                       │ 依賴的是「介面」
        ┌──────────────┼──────────────┬──────────────┐
        ▼              ▼              ▼              ▼
  OrderRepository  PaymentGateway  InventoryClient  NotificationService
        │              │              │              │
   正式環境:        正式環境:      正式環境:       正式環境:
   資料庫           第三方金流 API   庫存微服務      Email/簡訊
        │              │              │              │
   測試環境:        測試環境:      測試環境:       測試環境:
   Fake(記憶體)    Stub / Mock     Stub / Mock     Dummy / Spy / Mock
```

### 五種 Test Double 一覽

Meszaros 將 test double 分為五種,它們的差別在於「**替身為測試做了什麼事**」:

| 種類 | 一句話定義 | 主要職責 | 驗證方式 | 本專案範例 |
|---|---|---|---|---|
| **Dummy** | 佔位用,不會被真的用到 | 填滿參數列表 | 不驗證 | `DummyNotificationService` |
| **Stub** | 回傳預先寫死的答案 | 控制 SUT 的**間接輸入** | 狀態驗證 | `StubPaymentGateway` |
| **Spy** | 記錄 SUT 對它做的事 | 捕捉 SUT 的**間接輸出** | 事後翻紀錄斷言 | `SpyNotificationService` |
| **Mock** | 驗證互動是否如預期 | 驗證**行為**(參數/次數/順序) | 行為驗證 | Mockito `@Mock` |
| **Fake** | 有真功能的簡化實作 | 提供**行為一致**的輕量替代品 | 狀態驗證 | `FakeOrderRepository` |

> 口語上大家常把所有替身都叫「mock」,但精確區分這五種概念,能幫助你想清楚「這個測試到底在驗證什麼」。

---

## 2. 為什麼需要 Test Double?

直接用真實依賴來測試,會遇到這些問題:

1. **慢**:真實資料庫、HTTP 呼叫讓一個測試從毫秒級變成秒級;上千個測試就是災難。
2. **不穩定(flaky)**:網路抖動、外部服務維護,都會讓與程式碼無關的因素弄壞測試。
3. **有副作用**:你不會想在每次跑測試時真的刷一筆信用卡、真的寄一封信給客戶。
4. **難以製造情境**:要真實金流「回你一次餘額不足」「逾時一次」幾乎不可能;用替身只要一行。
5. **無法隔離錯誤**:整合在一起測,壞掉時你不知道是誰壞掉。單元測試要的是「這個單元」的對錯。
6. **還沒做出來**:協作模組可能由別的團隊開發中,先寫介面 + 替身,雙方就能並行開發。

Test double 讓測試**快速、確定性(deterministic)、可重現、情境可控**——這是單元測試的四大命脈。

---

## 3. 模擬案例:電商下訂單

被測對象 `OrderService.placeOrder()` 的業務流程:

```
顧客下單
   │
   ▼
1. 查庫存 (InventoryClient.isInStock)
   │  └─ 不足 → 丟 OutOfStockException(409)
   ▼
2. 建立訂單、向金流扣款 (PaymentGateway.charge)
   │  └─ 失敗 → 丟 PaymentFailedException(422)
   ▼
3. 扣庫存 (InventoryClient.deduct)、儲存訂單 (OrderRepository.save)
   ▼
4. 寄送訂單確認通知 (NotificationService.sendOrderConfirmation)
   ▼
回傳已成立 (CONFIRMED) 的訂單
```

四個協作者各自代表典型的依賴類型,也對應最適合的替身:

| 協作者 | 代表的真實依賴 | 依賴特性 | 適合的替身 |
|---|---|---|---|
| `PaymentGateway` | 第三方金流 API | 慢、會真的扣款、難製造失敗 | Stub(餵結果)/ Mock(驗金額) |
| `InventoryClient` | 另一個微服務 | 回傳值決定分支走向 | Stub / Mock |
| `OrderRepository` | 資料庫 | 「存了要查得回來」的語意很重要 | **Fake**(記憶體版) |
| `NotificationService` | Email / 簡訊 | void 方法,回傳值驗不到 | **Spy / Mock**;無關情境時當 **Dummy** |

---

## 4. 五種 Test Double 詳解

### 4.1 Dummy —— 佔位,不被使用

**定義**:只為了填滿參數列表而存在的物件,在測試情境中**不應該被呼叫到**。

**使用時機**:SUT 的建構子要求某個依賴,但你要測的路徑根本用不到它。

本專案的範例(`src/test/java/.../dummy/DummyTest.java`):測「付款失敗」時,流程在扣款就中斷了,永遠走不到「寄通知」,所以 `NotificationService` 只是個 Dummy:

```java
// DummyNotificationService:被呼叫就丟例外——測試通過即證明它真的沒被用到
public class DummyNotificationService implements NotificationService {
    @Override
    public void sendOrderConfirmation(Order order) {
        throw new UnsupportedOperationException("Dummy 不應該被呼叫!");
    }
}
```

```java
var service = new OrderService(
        fakeRepository,
        StubPaymentGateway.alwaysFail("餘額不足"),
        StubInventoryClient.alwaysInStock(),
        new DummyNotificationService());   // ← Dummy:純粹填參數

assertThatThrownBy(() -> service.placeOrder(cmd))
        .isInstanceOf(PaymentFailedException.class);
```

**實作技巧**:
- 最簡單的 Dummy 是傳 `null`,但容易混淆「故意不給」和「忘了給」;丟例外的 Dummy 更能表達意圖,還可順便抓到「情境其實有用到它」的設計誤判。
- 單一方法的介面可以用 lambda 當 Dummy:`(orderId, amount) -> { throw new UnsupportedOperationException(); }`。
- 用 Mockito 時,`mock(NotificationService.class)` 不加任何 stubbing/verify 也就是一個 Dummy。

---

### 4.2 Stub —— 餵答案,控制間接輸入

**定義**:對特定呼叫回傳**預先寫死(canned)的答案**,用來控制 SUT 的「間接輸入」(indirect input)——即 SUT 從協作者那裡「拿到」的資料。

**使用時機**:協作者的回傳值決定 SUT 的分支走向,而你想把 SUT 推進特定分支(成功/失敗/空清單/例外……)。

本專案的範例(`stub/StubTest.java`):

```java
var service = new OrderService(
        new FakeOrderRepository(),
        StubPaymentGateway.alwaysSucceed("TXN-12345"),  // Stub:寫死「付款成功」
        StubInventoryClient.alwaysInStock(),            // Stub:寫死「有貨」
        new SpyNotificationService());

Order order = service.placeOrder(new PlaceOrderCommand("SKU-MONITOR", 2, new BigDecimal("8000")));

// 狀態驗證:只檢查結果,整個測試沒有半句 verify
assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
assertThat(order.getTotalAmount()).isEqualByComparingTo("16000");
assertThat(order.getTransactionId()).isEqualTo("TXN-12345");
```

**關鍵心法**:
- Stub 搭配的是**狀態驗證**:安排好輸入 → 執行 → 檢查輸出/狀態。它自己不記錄、不驗證任何事。
- Stub 的威力在於「**便宜地製造罕見情境**」:要真實金流失敗一次很難,`alwaysFail("卡片已過期")` 只要一行。
- 餵「例外」也是一種間接輸入:Mockito 的 `thenThrow(...)` 可模擬逾時、連線中斷(見 `MockitoMockTest`)。
- Mockito 寫法:`when(gateway.charge(any(), any())).thenReturn(PaymentResult.success("TXN-1"))` —— 這時 mock 物件扮演的角色就是 Stub。

---

### 4.3 Spy —— 錄音,捕捉間接輸出

**定義**:一個會**記錄 SUT 對它做了什麼**的替身;測試結束後翻閱紀錄來斷言。它捕捉的是 SUT 的「間接輸出」(indirect output)——SUT 對外界「做出」但不反映在回傳值上的動作。

**使用時機**:協作者的方法是 `void`(寄信、發事件、寫 log),從 SUT 的回傳值完全看不出有沒有發生、發生了幾次、帶了什麼參數。

本專案的範例(`spy/SpyTest.java`):

```java
// SpyNotificationService:只錄不判
public class SpyNotificationService implements NotificationService {
    private final List<Order> receivedOrders = new ArrayList<>();

    @Override
    public void sendOrderConfirmation(Order order) {
        receivedOrders.add(order);   // 記錄下來,不做任何判斷
    }

    public int sentCount() { return receivedOrders.size(); }
    public Order lastOrder() { ... }
}
```

```java
service.placeOrder(cmd);

// 事後翻紀錄,用一般的 assert 驗證互動
assertThat(spyNotification.sentCount()).isEqualTo(1);
assertThat(spyNotification.lastOrder().getId()).isEqualTo(order.getId());
```

另一個同樣重要的方向——驗證「**不該發生的沒發生**」:

```java
// 付款失敗 → 一封信都不該寄(不然客戶會收到確認信卻查無訂單)
assertThatThrownBy(() -> service.placeOrder(cmd));
assertThat(spyNotification.sentCount()).isZero();
```

**Spy vs Mock 的差別**:兩者都在驗證互動,差別只在**斷言寫在哪裡**——
- Spy:替身只負責錄,**斷言寫在測試裡**(一般的 `assertThat`),失敗訊息直觀、不依賴框架。
- Mock:期望與驗證**內建在 mock 框架裡**(`verify(...)`)。

> ⚠️ 注意:Mockito 的 `@Spy` / `Mockito.spy()` 和這裡講的 Spy **不是同一件事**,見[第 6 節](#6-mockito-與五種分類的對照)。

---

### 4.4 Mock —— 驗證互動(行為驗證)

**定義**:預先設定「我期望被怎樣呼叫」,並在驗證階段檢查互動是否符合期望的替身。Mock 是**行為驗證**的代名詞。

**使用時機**:
- 測試的重點就是「**SUT 有沒有正確地使喚協作者**」——參數對不對、呼叫幾次、順序對不對。
- 尤其適合驗證**有副作用的指令型(command)互動**:扣款金額、扣庫存數量。

本專案的範例(`mock/MockitoMockTest.java`)展示了 Mockito 行為驗證的完整武器庫:

```java
@ExtendWith(MockitoExtension.class)
class MockitoMockTest {
    @Mock OrderRepository orderRepository;
    @Mock PaymentGateway paymentGateway;
    @Mock InventoryClient inventoryClient;
    @Mock NotificationService notificationService;

    @Test
    void placeOrder_success_behaviorVerification() {
        // Arrange:先讓 mock 兼任 Stub,餵出成功路徑需要的回答
        when(inventoryClient.isInStock("SKU-MONITOR", 2)).thenReturn(true);
        when(paymentGateway.charge(anyString(), any())).thenReturn(PaymentResult.success("TXN-MOCK-1"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0)); // 原樣回傳

        service.placeOrder(new PlaceOrderCommand("SKU-MONITOR", 2, new BigDecimal("8000")));

        // 1. 驗證參數:扣款金額必須是 8000 x 2
        verify(paymentGateway).charge(anyString(), eq(new BigDecimal("16000")));

        // 2. 驗證副作用:庫存扣對商品、對數量
        verify(inventoryClient).deduct("SKU-MONITOR", 2);

        // 3. ArgumentCaptor:捕捉參數深入檢查
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(notificationService).sendOrderConfirmation(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // 4. InOrder:必須「先存訂單、再寄通知」
        InOrder inOrder = inOrder(orderRepository, notificationService);
        inOrder.verify(orderRepository).save(any());
        inOrder.verify(notificationService).sendOrderConfirmation(any());
    }
}
```

驗證「沒發生」同樣是行為驗證的強項:

```java
verify(inventoryClient, never()).deduct(anyString(), anyInt());
verifyNoInteractions(orderRepository, notificationService);
```

**關鍵心法**:
- **只驗證對測試重要的互動**。把每一次呼叫都 `verify` 一遍,測試會和實作細節緊緊耦合,重構(即使行為不變)就會弄壞一片測試——這叫「脆弱測試(brittle test)」。
- 問自己:「這個互動如果錯了,使用者/下游系統會受害嗎?」會(扣錯錢、重複寄信)→ 值得 verify;不會(內部查了兩次 cache)→ 不要 verify。

---

### 4.5 Fake —— 真功能的簡化版

**定義**:介面的**完整可運作實作**,只是用了不適合上線的捷徑——最經典的就是「用 HashMap 代替資料庫」。

**使用時機**:
- 依賴的**行為一致性**很重要:「存進去的要查得回來」。
- 測試涉及**跨多次呼叫的流程**(先下單、再查詢),用 Stub 逐一編排回傳值既囉嗦又失真。

本專案的範例(`fake/FakeTest.java`):

```java
public class FakeOrderRepository implements OrderRepository {
    private final Map<String, Order> store = new HashMap<>();

    @Override
    public Order save(Order order) { store.put(order.getId(), order); return order; }

    @Override
    public Optional<Order> findById(String id) { return Optional.ofNullable(store.get(id)); }
}
```

```java
// 第一次互動:下單
Order placed = service.placeOrder(cmd);

// 第二次互動:查詢 —— Fake 讓「save 真的有存」這件事可被測到
Optional<Order> found = service.getOrder(placed.getId());
assertThat(found).isPresent();
assertThat(found.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
```

**Fake vs Stub 的差別**:Stub 回答是「寫死的」,跟 SUT 做了什麼無關;Fake 的回答是「算出來的」,忠實反映之前發生過的互動。

**實務建議**:
- Fake 本身是真的實作,**值得為它寫測試**。更進一步的做法是寫一份「契約測試(contract test)」,同一套測試同時跑真實實作與 Fake,確保兩者行為一致。
- H2 之於 PostgreSQL、Embedded Kafka 之於 Kafka、WireMock 之於真實 HTTP 服務,廣義上都是 Fake 的思路。

---

## 5. 狀態驗證 vs 行為驗證

這是理解 test double 最重要的分界線(Martin Fowler〈Mocks Aren't Stubs〉的核心):

| | 狀態驗證(State Verification) | 行為驗證(Behavior Verification) |
|---|---|---|
| 驗什麼 | 執行後的**結果**:回傳值、物件狀態 | 執行中的**互動**:呼叫了誰、什麼參數、幾次、什麼順序 |
| 用什麼替身 | Stub、Fake | Mock、Spy |
| 範例 | `assertThat(order.getStatus()).isEqualTo(CONFIRMED)` | `verify(gateway).charge(any(), eq(new BigDecimal("16000")))` |
| 優點 | 與實作解耦,重構不易壞 | 能驗證回傳值看不到的副作用 |
| 風險 | 測不到「過程中做錯事」(如重複扣款) | 容易與實作細節耦合,測試變脆弱 |

由此衍生出兩個學派:

- **古典學派(Classical / Detroit)**:能用真物件就用真物件,只在依賴「不方便」(慢、有副作用)時用替身,偏好狀態驗證。
- **倫敦學派(Mockist / London)**:凡依賴皆 mock,透過行為驗證由外而內驅動出物件間的協作設計。

**實務上的折衷(也是本專案採取的立場)**:
- **查詢(Query,有回傳值)用 Stub/Fake + 狀態驗證**;
- **指令(Command,void、有副作用)用 Mock/Spy + 行為驗證**;
- 值物件、純函式、自己團隊的簡單類別,直接用真的。

這其實就是「Command-Query 分離」原則在測試上的投影。

---

## 6. Mockito 與五種分類的對照

Mockito 是 Java 生態最主流的 mocking 框架(`spring-boot-starter-test` 內建)。要注意:**Mockito 的 API 名詞和 Meszaros 的分類並不一一對應**。

| Meszaros 概念 | Mockito 對應寫法 |
|---|---|
| Dummy | `mock(Foo.class)` 不做任何 stubbing / verify |
| Stub | `when(mock.method(...)).thenReturn(...)` / `thenThrow(...)` |
| Spy(錄互動) | `verify(...)` + `ArgumentCaptor`(錄下來給你斷言) |
| Mock(驗互動) | `verify(mock).method(...)`、`never()`、`times(n)`、`InOrder` |
| Fake | **Mockito 做不到**——Fake 要自己手寫(或用 H2、WireMock 這類工具) |

兩個最容易混淆的點:

1. **`Mockito.mock()` 產生的是「萬用替身」**:同一個物件,設定 `when(...)` 時它是 Stub,呼叫 `verify(...)` 時它是 Mock。所以「用了 Mockito」不代表「在做行為驗證」——重點是你的測試**驗了什麼**。

2. **Mockito 的 `@Spy` ≠ Meszaros 的 Spy**。Mockito 的 `spy()` 是「**部分模擬(partial mock)**」:包裝一個**真實物件**,預設走真實邏輯,但可以攔截覆寫部分方法、也可以 verify。Meszaros 的 Spy(本專案的 `SpyNotificationService`)則是純手寫的錄音機。需要 `Mockito.spy()` 的場合通常是在跟遺留代碼搏鬥;新設計若需要 partial mock,多半是類別職責太多的訊號。

### 手寫替身 vs Mockito,怎麼選?

| | 手寫替身 | Mockito |
|---|---|---|
| 可讀性 | 替身有名字、有意圖(`alwaysFail("餘額不足")`) | 設定語法樣板化,大量 `when/verify` 時較難讀 |
| 重複使用 | 一次手寫,多個測試共用 | 每個測試各自編排 |
| 彈性 | 新情境要改類別 | 任何回傳/例外/次數一行搞定 |
| Fake | 只能手寫 | 不支援 |
| 適合 | 專案核心、多測試共用的依賴 | 一次性情境、驗證複雜互動 |

兩者不互斥,本專案就是混用:核心四協作者有手寫替身,互動驗證交給 Mockito。

---

## 7. Spring 測試中的 Test Double

Spring 應用的依賴由容器裝配,因此「把替身塞進容器」有專門的工具。

### 7.1 不啟動 Spring:純單元測試(首選)

`OrderService` 用建構子注入,所以**大多數測試根本不需要 Spring**——直接 `new`,把替身從建構子塞進去(本專案的 Dummy/Stub/Spy/Mock/Fake 測試全是如此)。這是速度最快、回饋最即時的層級,應佔測試金字塔的最大宗。

> 設計準則:**讓業務邏輯可以在沒有框架的情況下被測試**。做得到,表示分層是健康的。

### 7.2 `@WebMvcTest`:切片測試 + `@MockitoBean`

只載入 Web 層(Controller、JSON 轉換、`@RestControllerAdvice`),Service 用 `@MockitoBean` 換成 mock 注入容器——專注測「HTTP 介面層」:路由、序列化、狀態碼、錯誤轉換。(`springtest/OrderControllerWebMvcTest.java`)

```java
@WebMvcTest(OrderController.class)
class OrderControllerWebMvcTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean OrderService orderService;   // 整個 Service 層換成 mock

    @Test
    void outOfStock_returns409() throws Exception {
        when(orderService.placeOrder(any())).thenThrow(new OutOfStockException("SKU-LAPTOP"));

        mockMvc.perform(post("/api/orders")...)
               .andExpect(status().isConflict());
    }
}
```

> `@MockitoBean` 是 Spring Boot 3.4+ / Spring Framework 6.2 的新註解,取代已棄用的 `@MockBean`(`@SpyBean` 同理,換成 `@MockitoSpyBean`)。語意相同:在容器裡用 Mockito mock **取代**同型別的 bean。

### 7.3 `@SpringBootTest`:整合測試,只替換「碰不得」的依賴

啟動完整容器,大部分 bean 用真的,只把外部依賴(金流)換成替身——兼顧「驗證 bean 裝配與真實協作」和「不打外部 API」。(`springtest/OrderServiceIntegrationTest.java`)

```java
@SpringBootTest
class OrderServiceIntegrationTest {
    @Autowired OrderService orderService;   // 真 Service + 真 Repository + 真 Inventory
    @MockitoBean PaymentGateway paymentGateway;   // 只有金流是替身

    @Test
    void placeOrder_withRealBeansExceptPayment() {
        when(paymentGateway.charge(any(), any())).thenReturn(PaymentResult.success("TXN-IT-1"));
        ...
    }
}
```

**注意成本**:每一種不同的 `@MockitoBean` 組合都會讓 Spring 建立一個新的 ApplicationContext(無法命中 context cache),測試套件會明顯變慢。能用純單元測試解決的,不要動用 `@SpringBootTest`。

### 7.4 更外圍的替身工具(延伸)

- **WireMock / MockWebServer**:在 HTTP 層架一個假伺服器,連 `RestClient`/`WebClient` 的序列化、逾時、重試都一起測到(比 mock 掉 client 介面更真實)。
- **Testcontainers**:用 Docker 跑真的 PostgreSQL/Kafka——嚴格說已經不是 test double,而是「真實依賴的拋棄式實例」,適合整合測試層。
- **H2 in-memory DB**:資料庫層級的 Fake。

---

## 8. 如何選擇:決策指南

```
這個依賴在測試情境中會被用到嗎?
 ├─ 不會 ────────────────────────────────→ Dummy
 └─ 會
     ├─ SUT 需要「從它拿資料」(查詢/間接輸入)
     │    ├─ 回答寫死就夠 ───────────────→ Stub
     │    └─ 需要跨呼叫的一致行為
     │       (存了要查得回來)───────────→ Fake
     └─ SUT 會「對它做事」(指令/間接輸出)
          ├─ 想用一般 assert、替身可重用 ──→ 手寫 Spy
          └─ 想驗參數/次數/順序 ─────────→ Mock (Mockito verify)
```

再加三條經驗法則:

1. **能用真物件就用真物件**。值物件(`Order`、`PaymentResult`)、純函式直接 new,替身永遠是手段而非目的。
2. **一個測試的 verify 越少越好**。每個測試聚焦一件事:驗狀態就少驗行為,驗行為就挑「做錯會出事」的互動驗。
3. **替身放在 port(介面)邊界**。不要 mock 到別人的類別內部;介面是你定義的契約,也是替身的天然安放處。

---

## 9. 常見反模式

| 反模式 | 症狀 | 解法 |
|---|---|---|
| **Over-mocking** | 連值物件、簡單類別都 mock,測試只是在驗證 mock 設定本身 | 值物件直接 new;只替換「不方便的依賴」 |
| **驗證所有互動** | 每個 mock 每次呼叫都 verify,一重構就紅一片 | 只 verify「做錯會出事」的互動 |
| **Mock 你不擁有的型別** | 直接 mock `RestTemplate`、第三方 SDK 類別,API 細節滲進所有測試 | 包一層自己的介面(如本專案的 `PaymentGateway`),mock 自己的介面;HTTP 層用 WireMock |
| **Stub 失真** | Stub 的回答違反真實依賴的契約(如 save 後 findById 仍回空) | 行為一致性重要時改用 Fake,並以契約測試對齊真實實作 |
| **測試邏輯洩漏到替身** | 手寫替身裡藏 if/else 與斷言,替身自己變成需要測試的程式 | 替身保持「笨」:Stub 只回答、Spy 只記錄 |
| **用 partial mock 掩蓋設計問題** | 需要 `Mockito.spy()` 覆寫 SUT 自己的一部分方法才測得動 | 把被覆寫的部分抽成獨立協作者,正常注入替身 |
| **替身與非同步/時間耦合** | 用 `Thread.sleep` 等待替身被呼叫 | 注入 `Clock`/executor,讓時間與排程也成為可替換的依賴 |

---

## 10. 專案結構與執行方式

```
src/main/java/com/example/order/
├── OrderApplication.java            # Spring Boot 進入點
├── domain/
│   ├── Order.java                   # 訂單(領域物件;測試中直接 new,不做替身)
│   └── OrderStatus.java
├── exception/
│   ├── OutOfStockException.java
│   └── PaymentFailedException.java
├── port/                            # ★ 依賴的抽象(介面)—— 替身的安放邊界
│   ├── OrderRepository.java
│   ├── PaymentGateway.java
│   ├── PaymentResult.java
│   ├── InventoryClient.java
│   └── NotificationService.java
├── service/
│   ├── OrderService.java            # ★ 被測對象(SUT)
│   └── PlaceOrderCommand.java
├── infra/                           # 正式環境的實作(教學用記憶體版)
│   ├── InMemoryOrderRepository.java
│   ├── SandboxPaymentGateway.java
│   ├── StockedInventoryClient.java
│   └── ConsoleNotificationService.java
└── web/
    ├── OrderController.java
    ├── GlobalExceptionHandler.java
    ├── PlaceOrderRequest.java
    └── OrderResponse.java

src/test/java/com/example/order/
├── doubles/                         # ★ 手寫的 test double(教學核心)
│   ├── DummyNotificationService.java   # Dummy:被呼叫就丟例外
│   ├── StubPaymentGateway.java         # Stub:回寫死的扣款結果
│   ├── StubInventoryClient.java        # Stub:回寫死的庫存狀態
│   ├── SpyNotificationService.java     # Spy:記錄收到的通知
│   └── FakeOrderRepository.java        # Fake:HashMap 版 Repository
├── dummy/DummyTest.java             # ① Dummy 示範
├── stub/StubTest.java               # ② Stub + 狀態驗證示範
├── spy/SpyTest.java                 # ③ Spy + 互動紀錄示範
├── mock/MockitoMockTest.java        # ④ Mockito Mock + 行為驗證示範
├── fake/FakeTest.java               # ⑤ Fake + 跨呼叫流程示範
└── springtest/
    ├── OrderControllerWebMvcTest.java      # @WebMvcTest + @MockitoBean
    └── OrderServiceIntegrationTest.java    # @SpringBootTest + 局部替身
```

### 執行

```bash
# 跑全部測試(14 個)
mvn test

# 只跑某一類示範
mvn test -Dtest=StubTest
mvn test -Dtest=MockitoMockTest

# 啟動應用程式(infra 內建模擬實作,可直接玩)
mvn spring-boot:run

# 下一筆訂單
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": "SKU-KEYBOARD", "quantity": 2, "unitPrice": 1500}'

# 用回傳的 id 查詢
curl http://localhost:8080/api/orders/{id}
```

### 建議閱讀順序

1. `service/OrderService.java` —— 先認識被測對象與它的四個協作者
2. `doubles/` —— 五個手寫替身,每個類別的 Javadoc 都說明了該替身的角色
3. `dummy → stub → spy → fake → mock` 五個測試,按序體會替身職責的遞進
4. `springtest/` —— 替身如何進入 Spring 容器

---

## 11. 參考資料

- Gerard Meszaros, *xUnit Test Patterns: Refactoring Test Code* —— Test Double 分類的原始出處
- Martin Fowler, [*Mocks Aren't Stubs*](https://martinfowler.com/articles/mocksArentStubs.html) —— 狀態驗證 vs 行為驗證、古典 vs 倫敦學派
- Martin Fowler, [*TestDouble*](https://martinfowler.com/bliki/TestDouble.html)
- [Mockito 官方文件](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing 官方文件](https://docs.spring.io/spring-boot/reference/testing/index.html) —— `@WebMvcTest`、`@SpringBootTest`、`@MockitoBean`
- Steve Freeman & Nat Pryce, *Growing Object-Oriented Software, Guided by Tests* —— 倫敦學派的經典
