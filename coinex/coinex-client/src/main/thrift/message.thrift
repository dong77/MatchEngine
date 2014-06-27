/**
 * Copyright {C} 2014 Coinport Inc. <http://www.coinport.com>
 *
 * WARNING:
 *  All structs must have at least 1 parameters, otherwise AKKA serialization fails.
 */

namespace java com.coinport.coinex.data

include "data.thrift"

///////////////////////////////////////////////////////////////////////
///////////////////////// PROCESSOR MESSAGES //////////////////////////

typedef data.ErrorCode                          _ErrorCode
typedef data.Currency                           _Currency
typedef data.Order                              _Order
typedef data.MarketDepth                        _MarketDepth
typedef data.UserAccount                        _UserAccount
typedef data.UserProfile                        _UserProfile
typedef data.AccountTransfer                    _AccountTransfer
typedef data.AccountTransfersWithMinerFee       _AccountTransfersWithMinerFee
typedef data.MarketSide                         _MarketSide
typedef data.ApiSecret                          _ApiSecret
typedef data.OrderInfo                          _OrderInfo
typedef data.Metrics                            _Metrics
typedef data.TransferStatus                     _TransferStatus
typedef data.Cursor                             _Cursor
typedef data.SpanCursor                         _SpanCursor
typedef data.EmailType                          _EmailType
typedef data.Transaction                        _Transaction
typedef data.CandleData                         _CandleData
typedef data.ChartTimeDimension                 _ChartTimeDimension
typedef data.QueryMarketSide                    _QueryMarketSide
typedef data.ExportedEventType                  _ExportedEventType
typedef data.HistoryAsset                       _HistoryAsset
typedef data.CurrentAsset                       _CurrentAsset
typedef data.HistoryPrice                       _HistoryPrice
typedef data.CurrentPrice                       _CurrentPrice
typedef data.TransferType                       _TransferType
typedef data.BitwayRequestType                  _BitwayRequestType
typedef data.RechargeCodeStatus                 _RechargeCodeStatus
typedef data.ABCodeItem                         _ABCodeItem
typedef data.CryptoCurrencyTransaction          _CryptoCurrencyTransaction
typedef data.BlockIndex                         _BlockIndex
typedef data.CryptoCurrencyTransferInfo         _CryptoCurrencyTransferInfo
typedef data.CryptoCurrencyBlock                _CryptoCurrencyBlock
typedef data.CryptoCurrencyAddressType          _CryptoCurrencyAddressType
typedef data.CryptoCurrencyTransferItem         _CryptoCurrencyTransferItem
typedef data.CashAccount                        _CashAccount
typedef data.Notification                       _Notification
typedef data.NotificationType                   _NotificationType
typedef data.CryptoCurrencyNetworkStatus        _CryptoCurrencyNetworkStatus
typedef data.AddressStatusResult                _AddressStatusResult
typedef data.CryptoAddress                      _CryptoAddress
typedef data.TFeeConfig                         _TFeeConfig
typedef data.Language                           _Language
typedef data.ReferralParams                     _ReferralParams

///////////////////////////////////////////////////////////////////////
// 'C' stands for external command,
// 'P' stands for persistent event derived from a external command,
// 'Q' for query,
// 'I' stands for inter-processor commands
// 'R+' stands for response to sender on command success,
// 'R-' stands for response to sender on command failure,
// 'R' stands for response to sender regardless of failure or success.

// WARNING: please avoid using map in event definitation, if you do, please
// make sure all map keys are either i64, i32, double, float, or string;
// do not struct as map keys so our serialization can still work.

////////// General
/* R-   */ struct MessageNotSupported                     {1: string event}

////////// Admin
/* R    */ struct AdminCommandResult                      {1: _ErrorCode error = data.ErrorCode.OK}
/* C,P  */ struct TakeSnapshotNow                         {1: string desc, 2: optional i32 nextSnapshotinSeconds}
/* C    */ struct DumpStateToFile                         {1: string actorPath}

////////// UserProcessor
/* C,P  */ struct DoRegisterUser                          {1: _UserProfile userProfile, 2: string password, 3: optional _ReferralParams rparams}
/* R-   */ struct RegisterUserFailed                      {1: _ErrorCode error}
/* R+   */ struct RegisterUserSucceeded                   {1: _UserProfile userProfile}

/* C,P  */ struct DoResendVerifyEmail                     {1: string email}
/* R-   */ struct ResendVerifyEmailFailed                 {1: _ErrorCode error}
/* R+   */ struct ResendVerifyEmailSucceeded              {1: i64 id, 2: string email}

/* C,P  */ struct VerifyEmail                             {1: string token}
/* R-   */ struct VerifyEmailFailed                       {1: _ErrorCode error}
/* R+   */ struct VerifyEmailSucceeded                    {1: i64 id, 2: string email}

/* C,P  */ struct DoUpdateUserProfile                     {1: _UserProfile userProfile}
/* R-   */ struct UpdateUserProfileFailed                 {1: _ErrorCode error}
/* R+   */ struct UpdateUserProfileSucceeded              {1: _UserProfile userProfile /* previous profile */}

/* C,P  */ struct DoRequestPasswordReset                  {1: string email, 2: optional string passwordResetToken /* ignored */}
/* R-   */ struct RequestPasswordResetFailed              {1: _ErrorCode error}
/* R+   */ struct RequestPasswordResetSucceeded           {1: i64 id, 2: string email}

/* Q    */ struct ValidatePasswordResetToken              {1: string passwordResetToken}
/* R    */ struct PasswordResetTokenValidationResult      {1: optional _UserProfile userProfile}

/* Q    */ struct DoSuspendUser                           {1: i64 userId}
/* R    */ struct SuspendUserResult                       {1: optional _UserProfile userProfile}

/* Q    */ struct DoResumeUser                            {1: i64 userId}
/* R    */ struct ResumeUserResult                        {1: optional _UserProfile userProfile}

/* C,P  */ struct DoResetPassword                         {1: string newPassword, 2: string passwordResetToken}
/* R-   */ struct ResetPasswordFailed                     {1: _ErrorCode error}
/* R+   */ struct ResetPasswordSucceeded                  {1: i64 id, 2: string email}

/* C    */ struct Login                                   {1: string email, 2: string password} // TODO: this may also be a persistent command
/* R-   */ struct LoginFailed                             {1: _ErrorCode error}
/* R+   */ struct LoginSucceeded                          {1: i64 id, 2: string email}

/* Q    */ struct VerifyGoogleAuthCode                    {1: string email, 2: i32 code}
/* R    */ struct GoogleAuthCodeVerificationResult        {1: optional _UserProfile userProfile}

/* C,P  */ struct DoRequestTransfer                       {1: _AccountTransfer transfer}
/* R-   */ struct RequestTransferFailed                   {1: _ErrorCode error}
/* R+   */ struct RequestTransferSucceeded                {1: _AccountTransfer transfer}

/* R-   */ struct CryptoTransferFailed                   {1: _AccountTransfer transfer, 2:_ErrorCode error}
/* R+   */ struct CryptoTransferSucceeded                {1: _TransferType txType, 2: list<_AccountTransfer> transfers, 3: optional i64 minerFee}
/* R+   */ struct CryptoTransferResult                   {1: map<string, _AccountTransfersWithMinerFee> multiTransfers}

/* C,P  */ struct DoCancelTransfer                        {1: _AccountTransfer transfer}
/* C,P  */ struct AdminConfirmTransferFailure             {1: _AccountTransfer transfer, 2:_ErrorCode error}
/* C,P  */ struct AdminConfirmTransferSuccess             {1: _AccountTransfer transfer}

/* C,P  */ struct DoRequestGenerateABCode                 {1: i64 userId, 2: i64 amount, 3: optional string a, 4: optional string b}
/* R-   */ struct RequestGenerateABCodeFailed             {1: _ErrorCode error}
/* R+   */ struct RequestGenerateABCodeSucceeded          {1: string codeA, 2: string codeB}

/* C,P  */ struct DoRequestACodeQuery                     {1: i64 userId, 2: string codeA}
/* R-   */ struct RequestACodeQueryFailed                 {1: _ErrorCode error}
/* R+   */ struct RequestACodeQuerySucceeded              {1: string codeA, 2: _RechargeCodeStatus status, 3: i64 amount}

/* C,P  */ struct DoRequestBCodeRecharge                  {1: i64 userId, 2: string codeB}
/* R-   */ struct RequestBCodeRechargeFailed              {1: _ErrorCode error}
/* R+   */ struct RequestBCodeRechargeSucceeded           {1: string codeB, 2: _RechargeCodeStatus status, 3: i64 amount}

/* C,P  */ struct DoRequestConfirmRC                      {1: i64 userId, 2: string codeB, 3: i64 amount}
/* R-   */ struct RequestConfirmRCFailed                  {1: _ErrorCode error}
/* R+   */ struct RequestConfirmRCSucceeded               {1: string codeB, 2: _RechargeCodeStatus status, 3: i64 amount}

/* R-   */ struct AddRobotDNAFailed                       {1: _ErrorCode error, 2: i64 dnaId}
/* R+   */ struct AddRobotDNASucceeded                    {1: i64 dnaId}

/* R-   */ struct RemoveRobotDNAFailed                    {1: _ErrorCode error, 2: string robotIds}
/* R+   */ struct RemoveRobotDNASucceeded                 {1: i64 dnaId}

/* C,P  */ struct DoSubmitOrder                           {1: _MarketSide side, 2: _Order order}
/* R-   */ struct SubmitOrderFailed                       {1: _MarketSide side, 2: _Order order, 3: _ErrorCode error}
/* I    */ struct OrderFundFrozen                         {1: _MarketSide side, 2: _Order order}

////////// ApiAuthProcessor
/* C,P  */ struct DoAddNewApiSecret                       {1: i64 userId}
/* C,P  */ struct DoDeleteApiSecret                       {1: _ApiSecret secret}
/* R    */ struct ApiSecretOperationResult                {1: _ErrorCode error, 2: list<_ApiSecret> secrets}

/* Q    */ struct QueryApiSecrets                         {1: i64 userId, 2: optional string identifier}
/* R    */ struct QueryApiSecretsResult                   {1: i64 userId, 2: list<_ApiSecret> secrets}


////////// MarketProcessor
/* C,P  */ struct DoCancelOrder                           {1: _MarketSide side, 2: i64 id, 3: i64 userId}
/* R-   */ struct CancelOrderFailed                       {1: _ErrorCode error}

/* I,R+ */ struct OrderSubmitted                          {1: _OrderInfo originOrderInfo, 2: list<_Transaction> txs}
/* I,R+ */ struct OrderCancelled                          {1: _MarketSide side, 2: _Order order}

////////// RobotProcessor commands
/* C,P  */ struct DoUpdateMetrics                         {1: _Metrics metrics}
/* C,P  */ struct DoAddRobotDNA                           {1: map<string, string> states}
/* C,P  */ struct DoRemoveRobotDNA                        {1: i64 dnaId}

////////// Mailer
/* C    */ struct DoSendEmail                             {1: string email, 2: _EmailType emailType, 3: map<string, string> params}

////////// BitwayProcessor
/* C    */ struct AllocateNewAddress                      {1: _Currency currency, 2: i64 userId, 3: optional string assignedAddress}
/* R    */ struct AllocateNewAddressResult                {1: _Currency currency, 2: _ErrorCode error = data.ErrorCode.OK, 3: optional string address}
/* C,I  */ struct TransferCryptoCurrency                  {1: _Currency currency, 2: list<_CryptoCurrencyTransferInfo> transferInfos, 3: _TransferType type}
/* R    */ struct TransferCryptoCurrencyResult            {1: _Currency currency, 2: _ErrorCode error = data.ErrorCode.OK, 3: optional TransferCryptoCurrency request}
/* C,I  */ struct MultiTransferCryptoCurrency             {1: _Currency currency, 2: map<_TransferType, list<_CryptoCurrencyTransferInfo>> transferInfos}
/* R    */ struct MultiTransferCryptoCurrencyResult       {1: _Currency currency, 2: _ErrorCode error = data.ErrorCode.OK, 3: optional map<_TransferType, list<_CryptoCurrencyTransferInfo>> transferInfos}
/* I    */ struct MultiCryptoCurrencyTransactionMessage   {1: _Currency currency, 2: list<_CryptoCurrencyTransaction> txs, 3: optional _BlockIndex reorgIndex}
/* Q    */ struct QueryCryptoCurrencyAddressStatus        {1: _Currency currency, 2: _CryptoCurrencyAddressType addressType}
/* R    */ struct QueryCryptoCurrencyAddressStatusResult  {1: _Currency currency, 2: map<string, _AddressStatusResult> status}
/* Q    */ struct QueryCryptoCurrencyNetworkStatus        {1: _Currency currency}
/* R    */ struct QueryCryptoCurrencyNetworkStatusResult  {1: _Currency currency, 2: _CryptoCurrencyNetworkStatus status}
/* Q    */ struct QueryReserveStatus                      {1: _Currency currency}
/* R    */ struct QueryReserveStatusResult                {1: _Currency currency, 2: map<_CryptoCurrencyAddressType, i64> amounts}
/* C    */ struct AdjustAddressAmount                     {1: _Currency currency, 2: string address, 3: i64 adjustAmount}
/* R    */ struct AdjustAddressAmountResult               {1: _Currency currency, 2: _ErrorCode error = data.ErrorCode.OK, 3: string address, 4: optional i64 adjustAmount}

////////// Bitway nodejs
/* C    */ struct CleanBlockChain                         {1: _Currency currency} // use this only if want re-start up from fatal new branch error. and must make sure the new coming block height is higher than previous highest block height
/* C    */ struct SyncHotAddresses                        {1: _Currency currency}
/* R    */ struct SyncHotAddressesResult                  {1: _ErrorCode error, 2: set<_CryptoAddress> addresses}
/* C    */ struct SyncPrivateKeys                         {1: _Currency currency, 2: optional set<string> pubKeys}
/* R    */ struct SyncPrivateKeysResult                   {1: _ErrorCode error, 2: set<_CryptoAddress> addresses}
/* C    */ struct GetMissedCryptoCurrencyBlocks           {1: list<_BlockIndex> startIndexs, 2: _BlockIndex endIndex} // returned (startIndex, endIndex]
/* C    */ struct GenerateAddresses                       {1: i32 num}
/* R    */ struct GenerateAddressesResult                 {
                                                              1: _ErrorCode error,
                                                              2: optional set<_CryptoAddress> addresses,
                                                              3: optional _CryptoCurrencyAddressType addressType
                                                          }
/* I    */ struct CryptoCurrencyBlockMessage              {1: optional _BlockIndex reorgIndex, /* BlockIndex(None, None) means in another branch */ 2: _CryptoCurrencyBlock block, 3: optional i64 timestamp}
/* C    */ struct BitwayRequest                           {
                                                              1: _BitwayRequestType type
                                                              2: _Currency currency
                                                              3: optional GenerateAddresses generateAddresses
                                                              4: optional GetMissedCryptoCurrencyBlocks getMissedCryptoCurrencyBlocksRequest
                                                              5: optional TransferCryptoCurrency transferCryptoCurrency
                                                              6: optional SyncHotAddresses syncHotAddresses
                                                              7: optional MultiTransferCryptoCurrency multiTransferCryptoCurrency
                                                              8: optional SyncPrivateKeys syncPrivateKeys
                                                          }
/* I    */ struct BitwayMessage                           {
                                                              1: _Currency currency
                                                              2: optional GenerateAddressesResult generateAddressResponse
                                                              3: optional _CryptoCurrencyTransaction tx
                                                              4: optional CryptoCurrencyBlockMessage blockMsg
                                                              5: optional SyncHotAddressesResult syncHotAddressesResult
                                                              6: optional SyncPrivateKeysResult syncPrivateKeysResult
                                                          }

////////////////////////////////////////////////////////////////
//////////////////////// VIEW MESSAGES /////////////////////////
////////////////////////////////////////////////////////////////

////////// AccountView
/* Q    */ struct QueryProfile                        {1: optional i64 uid, 2: optional string email}
/* R    */ struct QueryProfileResult                  {1: optional _UserProfile userProfile}
/* Q    */ struct QueryAccount                        {1: i64 userId}
/* R    */ struct QueryAccountResult                  {1: _UserAccount userAccount}
/* Q    */ struct QueryAccountStatistics              {1: _Currency currency}
/* R    */ struct QueryAccountStatisticsResult        {1: optional _CashAccount aggregation, 2: optional _CashAccount hotWallet, 3: optional _CashAccount coldWallet, 4: optional double reserveRatio}
/* Q    */ struct QueryRCDepositRecord                {1: i64 userId}
/* R    */ struct QueryRCDepositRecordResult          {1: list<_ABCodeItem> items}
/* Q    */ struct QueryRCWithdrawalRecord             {1: i64 userId}
/* R    */ struct QueryRCWithdrawalRecordResult       {1: list<_ABCodeItem> items}
/* R    */ struct QueryFeeConfigResult                {1: _TFeeConfig feeConfig}

////////// MarketDepthView
/* Q    */ struct QueryMarketDepth                        {1: _MarketSide side, 2: i32 maxDepth}
/* Q    */ struct QueryMarketDepthByPrice                 {1: _MarketSide side, 2: double askPrice, 3: double bidPrice}
/* R    */ struct QueryMarketDepthResult                  {1: _MarketDepth marketDepth}

/* Q    */ struct DoSimulateOrderSubmission               {1: DoSubmitOrder doSubmitOrder}
/* R+   */ struct OrderSubmissionSimulated                {1: OrderSubmitted orderSubmitted}

////////// CandleDataView
/* Q    */ struct QueryCandleData                         {1: _MarketSide side, 2: _ChartTimeDimension dimension, 3: i64 from, 4: i64 to}
/* R    */ struct QueryCandleDataResult                   {1: _CandleData candleData}

////////// OrderView
/* Q    */ struct QueryOrder                              {1: optional i64 uid, 2: optional i64 oid, 3:optional i32 status, 4:optional _QueryMarketSide side, 5: _Cursor cursor}
/* R    */ struct QueryOrderResult                        {1: list<_OrderInfo> orderinfos, 2: i64 count}

////////// TransactionView
/* Q    */ struct QueryTransaction                        {1: optional i64 tid, 2: optional i64 uid, 3: optional i64 oid, 4:optional _QueryMarketSide side, 5: _Cursor cursor}
/* R    */ struct QueryTransactionResult                  {1: list<_Transaction> transactions, 2: i64 count}

////////// which view?
/* Q    */ struct QueryTransfer                           {1: optional i64 uid, 2: optional _Currency currency, 3: optional _TransferStatus status, 4: optional _SpanCursor spanCur, 5:list<_TransferType> types, 6: _Cursor cur}
/* R    */ struct QueryTransferResult                     {1: list<_AccountTransfer> transfers, 2: i64 count}

////////// which view?
/* Q    */ struct QueryAsset                              {1: i64 uid, 2: i64 from, 3: i64 to}
/* R    */ struct QueryAssetResult                        {1: _CurrentAsset currentAsset, 2: _HistoryAsset historyAsset, 3: _CurrentPrice currentPrice, 4: _HistoryPrice historyPrice}

////////// query crypto currency transfer item
/* Q    */ struct QueryCryptoCurrencyTransfer             {
                                                            1: optional i64 id,
                                                            2: optional string sigId,
                                                            3: optional string txid,
                                                            4: optional _Currency currency,
                                                            5: optional _TransferType txType,
                                                            6: optional _TransferStatus status
                                                            7: optional _SpanCursor spanCur
                                                            8:  _Cursor cur
                                                          }
/* R    */ struct QueryCryptoCurrencyTransferResult       {1: list<_CryptoCurrencyTransferItem> transfers, 2: i64 count}

////////// notification
/* C    */ struct SetNotification                         {1: _Notification notification}
/* Q    */ struct QueryNotification                       {1: optional i64 id, 2: optional _NotificationType ntype, 3: optional bool getRemoved, 4: optional _Language lang, 5: _Cursor cur}
/* R    */ struct QueryNotificationResult                 {1: list<_Notification> notifications, 2: i64 count}

////////// monitor
/* Q    */ struct QueryActiveActors                       {}
/* R    */ struct QueryActiveActorsResult                 {1: map<string, list<string>> actorPaths}
