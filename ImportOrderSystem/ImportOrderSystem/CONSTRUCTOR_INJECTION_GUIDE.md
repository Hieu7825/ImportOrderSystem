# Constructor Injection Pattern - ImportOrderSystem

## Overview

Tất cả Services đã được refactor để **hỗ trợ Constructor Injection**, cho phép dễ dàng inject Fake repositories trong tests.

## Pattern: Dual Constructors

Mỗi Service có **2 constructors**:

1. **Parametrized Constructor** - Cho Dependency Injection (Testing)
2. **No-arg Constructor** - Default production behavior

### Example: SiteOrderService

```java
public class SiteOrderService {

    private final SiteOrderRepository  siteOrderRepo;
    private final FinalOrderRepository finalOrderRepo;
    private final SubBatchRepository   subBatchRepo;
    private final OrderOptimizationService optimService;
    private final SiteRepository siteRepo;

    // ✅ Constructor for Dependency Injection (for testing)
    public SiteOrderService(
            SiteOrderRepository siteOrderRepo,
            FinalOrderRepository finalOrderRepo,
            SubBatchRepository subBatchRepo,
            OrderOptimizationService optimService,
            SiteRepository siteRepo) {
        this.siteOrderRepo = siteOrderRepo;
        this.finalOrderRepo = finalOrderRepo;
        this.subBatchRepo = subBatchRepo;
        this.optimService = optimService;
        this.siteRepo = siteRepo;
    }

    // ✅ Default Constructor (for production)
    public SiteOrderService() {
        this.siteOrderRepo = new SiteOrderRepository();
        this.finalOrderRepo = new FinalOrderRepository();
        this.subBatchRepo = new SubBatchRepository();
        this.optimService = new OrderOptimizationService();
        this.siteRepo = new SiteRepository();
    }
}
```

## Services Refactored ✅

### Core Services (6 services)

1. **SiteOrderService** - 5 parameters (test class: UC1, UC2, UC3)
2. **WarehouseService** - 3 parameters (test class: UC5)
3. **OrderRequestService** - 2 parameters (test class: UC4)
4. **OrderOptimizationService** - 6 parameters (test class: UC6)
5. **AuthService** - 1 parameter
6. **UserManagementService** - 2 parameters

### Additional Services (4 services)

7. **SiteService** - 3 parameters
8. **MerchandiseService** - 2 parameters
9. **StockQueryService** - 1 parameter

## Usage in Tests

### Before (Hardcoded Dependencies)

```java
@BeforeEach
void setUp() {
    // ❌ Cannot inject Fake repositories
    siteOrderService = new SiteOrderService();
}
```

### After (Constructor Injection)

```java
@BeforeEach
void setUp() {
    // ✅ Create Fake repositories
    fakeSiteOrderRepo = new FakeSiteOrderRepository();
    fakeFinalOrderRepo = new FakeFinalOrderRepository();
    fakeSubBatchRepo = new FakeSubBatchRepository();

    // ✅ Inject into Service
    siteOrderService = new SiteOrderService(
        fakeSiteOrderRepo,
        fakeFinalOrderRepo,
        fakeSubBatchRepo,
        null,  // or pass fake if needed
        null
    );
}

@Test
void testConfirmBySite_Success() {
    // Setup test data
    SiteOrder mockOrder = new SiteOrder();
    mockOrder.setSiteOrderId("SO-123");
    mockOrder.setStatus("SENT");
    mockOrder.setConfirmedBySite(false);

    // Save to Fake repo
    fakeSiteOrderRepo.save(mockOrder);

    // Execute
    siteOrderService.confirmBySite("SO-123");

    // Verify
    SiteOrder result = fakeSiteOrderRepo.findBySiteOrderId("SO-123");
    assertTrue(result.isConfirmedBySite());
}
```

## Production Code (No Changes)

Existing production code **không bị ảnh hưởng**:

```java
// Production code - still works!
SiteOrderService service = new SiteOrderService();  // Uses default constructor
service.confirmBySite("SO-123");
```

## Key Benefits

✅ **Testability** - Easy to inject mock/fake objects  
✅ **No Breaking Changes** - Default constructor maintains compatibility  
✅ **Flexibility** - Can pass null for unused dependencies  
✅ **Clear Intent** - DI constructor shows explicit dependencies  
✅ **SOLID Principles** - Follows Dependency Inversion Principle

## Constructor Signatures

### SiteOrderService

```
SiteOrderService(
    SiteOrderRepository siteOrderRepo,
    FinalOrderRepository finalOrderRepo,
    SubBatchRepository subBatchRepo,
    OrderOptimizationService optimService,
    SiteRepository siteRepo)
```

### WarehouseService

```
WarehouseService(
    SiteOrderRepository siteOrderRepo,
    FinalOrderRepository finalOrderRepo,
    DiscrepancyRepository discrepancyRepo)
```

### OrderRequestService

```
OrderRequestService(
    OrderRequestRepository orderRepo,
    MerchandiseRepository merchRepo)
```

### OrderOptimizationService

```
OrderOptimizationService(
    SiteRepository siteRepo,
    StockRepository stockRepo,
    OrderRequestRepository orderRepo,
    SiteOrderRepository siteOrderRepo,
    FinalOrderRepository finalOrderRepo,
    SubBatchRepository subBatchRepo)
```

### AuthService

```
AuthService(UserRepository userRepo)
```

### UserManagementService

```
UserManagementService(
    UserRepository userRepo,
    SiteOrderRepository siteOrderRepo)
```

### SiteService

```
SiteService(
    SiteRepository siteRepo,
    SiteOrderRepository siteOrderRepo,
    UserRepository userRepo)
```

### MerchandiseService

```
MerchandiseService(
    MerchandiseRepository merchRepo,
    OrderRequestRepository orderRepo)
```

### StockQueryService

```
StockQueryService(StockRepository stockRepo)
```

## Migration Path

1. ✅ **Phase 1**: All Services have DI constructors
2. ✅ **Phase 2**: All test classes use DI constructors
3. ✅ **Phase 3**: Can add more detailed tests if needed
4. ✅ **Phase 4**: Production code unaffected (uses default constructor)

## Next Steps

1. Run `mvn test` to verify all tests compile and pass
2. Existing production code continues to work (no changes needed)
3. New tests can easily inject fake repositories
4. Controllers can also be refactored to use DI if needed
