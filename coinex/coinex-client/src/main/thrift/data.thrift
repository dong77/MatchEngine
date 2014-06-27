/**
 * Copyright {C} 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

namespace java com.coinport.coinex.data

///////////////////////////////////////////////////////////////////////
///////////////////////////// ERROR CODES /////////////////////////////
///////////////////////////////////////////////////////////////////////

enum ErrorCode {
    OK = 0
    UNKNOWN = 1

    // User related
    EMAIL_ALREADY_REGISTERED         = 1001
    MISSING_INFORMATION              = 1002
    USER_NOT_EXIST                   = 1003
    PASSWORD_NOT_MATCH               = 1004
    TOKEN_NOT_MATCH                  = 1005
    EMAIL_NOT_VERIFIED               = 1006

    // Account related
    PRICE_OUT_OF_RANGE               = 2001
    INSUFFICIENT_FUND                = 2002
    INVALID_AMOUNT                   = 2003
    LOCKED_A_CODE                    = 2004
    USED_B_CODE                      = 2005
    INVALID_B_CODE                   = 2006
    UNSUPPORT_TRANSFER_TYPE          = 2007
    BITWAY_PROCESS_FAIL              = 2009
    INSUFFICIENT_HOT                 = 2010
    INSUFFICIENT_COLD                = 2011

    // Market related
    ORDER_NOT_EXIST                  = 3001

    // Api Auth related
    TOO_MANY_SECRETS                 = 5001
    INVALID_SECRET                   = 5002

    // Deposit/Withdrawal
    ALREADY_CONFIRMED                = 6001
    TRANSFER_NOT_EXIST               = 6002
    USER_CANCELED                    = 6003
    USER_AUTHEN_FAIL                 = 6004

    // Robot
    ROBOT_DNA_EXIST                  = 7001
    ROBOT_DNA_IN_USE                 = 7002

    // Bitway
    NOT_ENOUGH_ADDRESS_IN_POOL       = 8001
    INVALID_REQUEST_ADDRESS_NUM      = 8002
    RPC_ERROR                        = 8003
    NO_ADDRESS_FOUND                 = 8004
    WITHDRAWAL_TO_DEPOSIT_ADDRESS    = 8005
    ADDRESS_FAIL                     = 8006 // NO_ADDRESS_FOUND or WITHDRAWAL_TO_DEPOSIT_ADDRESS

    // Controller && Services
    PARAM_EMPTY                      = 9001
    CAPTCHA_NOT_MATCH                = 9002
    INVALID_EMAIL_FORMAT             = 9003
    INVALID_PASSWORD_FORMAT          = 9004
    SMS_CODE_NOT_MATCH               = 9005
    MOBILE_NOT_VERIFIED              = 9006
    SEND_SMS_FREQUENCY_TOO_HIGH      = 9007
    EMAIL_NOT_BIND_WITH_INVITE_CODE  = 9008

    // Transfer
    TRANSFER_REJECT                  = 10001
}


///////////////////////////////////////////////////////////////////////
///////////////////////// PERSISTENT ENUMS ////////////////////////////
///////////////////////////////////////////////////////////////////////
enum Currency {
    UNKNOWN = 0
    CNY = 1
    USD = 2

    // must make sure that the min index of crypto currency is 1000
    BTC  = 1000
    LTC  = 1010
    PTS  = 1200
    DOGE = 1100
    DRK  = 1300
}

enum OrderStatus {
    PENDING = 0
    PARTIALLY_EXECUTED = 1
    FULLY_EXECUTED = 2
    CANCELLED = 3
    CANCELLED_BY_MARKET = 4
    PARTIALLY_EXECUTED_THEN_CANCELLED_BY_MARKET = 5
    UNKNOWN = 10
}

enum UserStatus {
    NORMAL = 0
    SUSPENDED = 1
}

enum EmailType {
    REGISTER_VERIFY = 1
    LOGIN_TOKEN = 2
    PASSWORD_RESET_TOKEN = 3
    MONITOR = 4
}

enum ChartTimeDimension {
    ONE_MINUTE = 1
    THREE_MINUTES = 2
    FIVE_MINUTES = 3
    FIFTEEN_MINUTES = 4
    THIRTY_MINUTES = 5
    ONE_HOUR = 6
    TWO_HOURS = 7
    FOUR_HOURS = 8
    SIX_HOURS = 9
    TWELVE_HOURS = 10
    ONE_DAY = 11
    THREE_DAYS = 12
    ONE_WEEK = 13
}

enum Direction {
    UP   = 1
    DOWN = 2
    KEEP = 3
}

enum TransferType {
    DEPOSIT     = 0
    WITHDRAWAL  = 1
    USER_TO_HOT = 2
    HOT_TO_COLD = 3
    COLD_TO_HOT = 4
    UNKNOWN     = 5
}

enum RechargeCodeStatus {
    UNUSED = 0
    FROZEN = 1
    CONFIRMING = 2
    RECHARGE_DONE = 3
}

enum TransferStatus {
    PENDING              = 0
    ACCEPTED             = 1
    CONFIRMING           = 2
    CONFIRMED            = 3
    SUCCEEDED            = 4
    FAILED               = 5 // this will happen when confirmation satisfied but can't spend it
    REORGING             = 6
    REORGING_SUCCEEDED   = 7
}

enum ExportedEventType {
    ACCOUNT_EVENT = 0
    MARKET_EVENT  = 1
}

enum RefundReason {
    DUST                   = 0
    HIT_TAKE_LIMIT         = 1
    AUTO_CANCELLED         = 2
    OVER_CHARGED           = 3
    HIT_TRANSACTION_LIMIT  = 4
}

enum CryptoCurrencyAddressType {
    USER   = 0
    UNUSED = 1
    HOT    = 2
    COLD   = 3
}

enum BitwayRequestType {
    GENERATE_ADDRESS   = 0
    TRANSFER           = 1
    GET_MISSED_BLOCKS  = 2
    SYNC_HOT_ADDRESSES = 3
    MULTI_TRANSFER     = 4
    SYNC_PRIVATE_KEYS  = 5
}

enum BitwayResponseType {
    GENERATE_ADDRESS   = 0
    TRANSACTION        = 1
    TRANSFER           = 2
    AUTO_REPORT_BLOCKS = 3
    GET_MISSED_BLOCKS  = 4
    SYNC_HOT_ADDRESSES = 5
    SYNC_PRIVATE_KEYS  = 6
}

enum NotificationType {
    SUCCESS = 0
    INFO    = 1
    WARNING = 2
    DANGER  = 3
}

enum Language {
    CHINESE = 0
    ENGLISH = 1
}

///////////////////////////////////////////////////////////////////////
////////////////////////// PERSISTENT DATA ////////////////////////////
///////////////////////////////////////////////////////////////////////

// RD stands reciprocal Double
// RDouble(12.3, true) == 1/12.3, RDouble(12.3, false) == 12.3
struct RDouble {
    1: double input
    2: bool rm // reciprocal mark
}

struct UserProfile {
    1:  i64 id
    2:  string email
    3:  optional string realName
    4:  optional string nationalId
    5:  optional string passwordHash
    6:  bool emailVerified
    8:  optional string mobile
    9:  bool mobileVerified
    10: optional string passwordResetToken
    11: optional string verificationToken
    12: optional string loginToken
    13: optional string googleAuthenticatorSecret
    14: UserStatus status
    15: optional map<Currency, string> depositAddresses
    16: optional map<Currency, string> withdrawalAddresses
    17: optional i64 referralToken
    18: optional i64 referralUserId  // this should be hidden for this user
}

struct ReferralParams {
    1: optional i64 referralToken
}

struct MarketSide {
    1: Currency outCurrency
    2: Currency inCurrency
}

struct TFeeRule {
    1: optional i64 fee
    2: optional double percentage
}

struct TFeeConfig {
    1: map<MarketSide, TFeeRule> marketFeeRules
    2: map<i32, TFeeRule> robotFeeRules
    3: map<Currency, TFeeRule> transferFeeRules
}

struct Fee {
    1: i64 payer
    2: optional i64 payee  // pay to coinport if None
    3: Currency currency
    4: i64 amount
    5: optional string basis
}

struct Refund {
    1: RefundReason reason
    2: i64 amount
}

struct Order {
    1: i64 userId
    2: i64 id
    3: i64 quantity
    4: optional RDouble price
    5: optional i64 takeLimit
    6: optional i64 timestamp
    7: optional i32 robotType
    8: optional i64 robotId
    9: optional bool onlyTaker
    10: i64 inAmount = 0
    11: optional Refund refund
}

struct OrderInfo {
    1: MarketSide side
    2: Order order
    3: i64 outAmount
    4: i64 inAmount
    5: OrderStatus status
    6: optional i64 lastTxTimestamp
}

struct OrderUpdate {
    1: Order previous
    2: Order current
}

struct Transaction {
    1: i64 id
    2: i64 timestamp
    3: MarketSide side
    4: OrderUpdate takerUpdate
    5: OrderUpdate makerUpdate
    6: optional list<Fee> fees
}

struct CashAccount {
    1: Currency currency
    2: i64 available
    3: i64 locked
    4: i64 pendingWithdrawal
}

struct UserAccount {
    1: i64 userId
    2: map<Currency, CashAccount> cashAccounts
}

struct UserLogsState {
    1: map<i64, list<OrderInfo>> orderInfoMap
}

struct MarketDepthItem {
    1: double price
    2: i64 quantity
    3: optional list<i64> orderIds
}

struct MarketDepth {
    1: MarketSide side
    2: list<MarketDepthItem> asks
    3: list<MarketDepthItem> bids
}

struct CandleDataItem {
    1: i64 timestamp
    2: i64 inAoumt
    3: i64 outAoumt
    4: double open
    5: double close
    6: double low
    7: double high
}

struct CandleData {
    1: list<CandleDataItem> items
    2: MarketSide side
}

struct MetricsByMarket {
    1: MarketSide side
    2: double price  // 当前价格

    // ------------- 一段时间内（24 小时） ----------
    3: optional double low
    4: optional double high
    5: i64 volume = 0
    6: optional double gain  // 涨幅百分比

    7: Direction direction = Direction.KEEP
}

struct Metrics {
    1: map<MarketSide, MetricsByMarket> metricsByMarket
}

struct ApiSecret {
    1: string secret
    2: optional string identifier
    3: optional i64 userId
}

struct AccountTransfer {
    1:  i64 id
    2:  i64 userId
    3:  TransferType type
    4:  Currency currency
    5:  i64 amount
    6:  TransferStatus status = TransferStatus.PENDING
    7:  optional i64 created
    8:  optional i64 updated
    9:  optional ErrorCode reason
    10: optional Fee fee
    11: optional string address
    12: optional i64 confirm
    13: optional string txid
}

struct AccountTransfersWithMinerFee {
    1: list<AccountTransfer> transfers
    2: optional i64 minerFee
}

struct ABCodeItem {
    1: i64 id
    2: optional i64 dUserId
    3: i64 wUserId
    4: string codeA
    5: string codeB
    6: RechargeCodeStatus status
    7: i64 amount
    8: optional i64 queryExpireTime
    9: optional i64 created
    10: optional i64 updated
}

struct Cursor {
    1: i32 skip
    2: i32 limit
}

struct SpanCursor {
    1: i64 from
    2: i64 to
}

struct RedeliverFilterData {
    1: list<i64> processedIds
    2: i32 maxSize
}

struct RedeliverFilters {
    1: map<string, RedeliverFilterData> filterMap
}

struct QueryMarketSide {
    1: MarketSide side
    2: bool bothSide
}

//TODO(?): need a better name
struct MarketEvent {
    1: optional double price
    2: optional i64 volume
    3: optional i64 timestamp
}

struct TWindowQueue {
    1: i64 range
    2: i64 interval
    3: list<MarketEvent> elems
    4: i32 head
    5: i64 lastTick
}

struct TWindowVector {
    1: i64 range
    2: list<MarketEvent> elems
}

struct TStackQueue {
    1: list<double> elems
}

struct TMetricsObserver {
    1: MarketSide side
    2: TWindowVector transactionQueue
    3: TStackQueue minMaintainer
    4: TStackQueue maxMaintainer
    5: TStackQueue preMaintainer
    6: optional double price
    7: optional double lastPrice
    8: i64 volumeMaintainer
}

struct CurrentAsset {
    1: map<Currency, i64> currentAsset
}

struct HistoryAsset {
    1: map<i64, map<Currency, i64>> currencyMap
}

//TODO(?): need a better name
struct CurrentPrice {
    1: map<MarketSide, double> priceMap
}

//TODO(?): need a better name
struct HistoryPrice {
    1: map<MarketSide, map<i64, double>> priceMap
}

struct ExportOpenDataMap {
    1: map<string, i64> processorSeqMap
}

struct BlockIndex {
    1: optional string id
    2: optional i64 height
}

struct AddressStatusResult {
    1: optional string txid // last tx with include the address
    2: optional i64 height // tx first included block height
    3: i64 confirmedAmount
}

struct CryptoAddress {
    1: string address
    2: optional string privateKey
}

struct TAddressStatus {
    1: optional string txid // last tx with include the address
    2: optional i64 height // tx first included block height
    3: map<i64, list<i64>> books
}

// We have a case-class named Robot
struct TRobot {
    1: i64 robotId
    2: i64 userId
    3: i64 timestamp
    4: binary payloads
    5: string currentState
    6: i64 dnaId
}

struct CryptoCurrencyTransactionPort {
    1: string address
    2: optional double amount
    3: optional i64 internalAmount
    4: optional i64 userId
}

struct CryptoCurrencyTransferInfo {
    1: i64 id
    2: optional string to
    3: optional i64 internalAmount
    4: optional double amount
    5: optional string from
}

struct CryptoCurrencyTransaction {
    1:  optional string sigId
    2:  optional string txid
    3:  optional list<i64> ids
    4:  optional list<CryptoCurrencyTransactionPort> inputs
    5:  optional list<CryptoCurrencyTransactionPort> outputs
    6:  optional BlockIndex prevBlock
    7:  optional BlockIndex includedBlock
    8:  optional TransferType txType
    9:  TransferStatus status
    10: optional i64 timestamp
    11: optional i64 minerFee
}

struct CryptoCurrencyBlock {
    1: BlockIndex index
    2: BlockIndex prevIndex
    3: list<CryptoCurrencyTransaction> txs
}

struct CryptoCurrencyTransferItem {
    1: i64 id
    2: Currency currency
    3: optional string sigId
    4: optional string txid
    5: optional i64 userId
    6: optional CryptoCurrencyTransactionPort from
    7: optional CryptoCurrencyTransactionPort to
    8: optional BlockIndex includedBlock
    9: optional TransferType txType
    10: optional TransferStatus status
    11: optional i64 userToHotMapedDepositId
    12: optional i64 accountTransferId
    13: optional i64 created
    14: optional i64 updated
    15: optional i64 minerFee
}

struct CryptoCurrencyNetworkStatus {
    1: optional string id
    2: optional i64 height
    3: optional i64 heartbeatTime
    4: optional i64 queryTimestamp
}

struct Notification {
    1: i64 id
    2: NotificationType nType
    3: string title
    4: string content
    5: bool removed
    6: i64 created
    7: i64 updated
    8: Language lang
}
