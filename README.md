#About

Coinport is a fully open and transparent cryptocurrency exchange (https://coinport.com). This repository contains all core logic and code for our match engine (ME).

#Compile, Test, and Run

Coinport's ME use Scala as the main language. In order to compile the code, you need to have sbt installed. Please checkout sbt's official site for more information: http://www.scala-sbt.org. All following commands should be run inside the top level directory named `coinex`.

- To compile the code, run `sbt compile`.
- To run all the tests, run `sbt test`. No that you need to have network access as the testing framework will try to download the latest version of MongoDB to store system events and states.
- To start the ME, you need to have a MongoDB started on its default port, then run:


		sbt "project coinex-backend" "runMain com.coinport.coinex.CoinexApp 25551 127.0.0.1:25551 * 127.0.0.1 keyconfig"

When the ME started, you will see someting like this:

```
============= Akka Node Ready =============
with hostname: 127.0.0.1
with seeds: "akka.tcp://coinex@127.0.0.1:25551"
with roles: account_processor,account_transfer_mongo_reader,account_transfer_processor,account_view,api_auth_processor,api_auth_view,asset_view,bitway_processor_btc,bitway_processor_doge,bitway_processor_ltc,bitway_receiver_btc,bitway_receiver_doge,bitway_receiver_ltc,bitway_view_btc,bitway_view_doge,bitway_view_ltc,candle_data_view_doge-btc,candle_data_view_ltc-btc,mailer,market_depth_view_doge-btc,market_depth_view_ltc-btc,market_processor_doge-btc,market_processor_event_export_doge-btc,market_processor_event_export_ltc-btc,market_processor_ltc-btc,market_update_processor,metrics_view,monitor_service,notification_mongo,opendata_exporter,order_mongo_reader,order_mongo_writer,robot_processor,transaction_mongo_reader,transaction_mongo_writer,user_mongo_writer,user_processor


                                            _
             (_)                           | |
   ___  ___   _  _ __   _ __    ___   _ __ | |_ __  __
  / __|/ _ \ | || '_ \ | '_ \  / _ \ | '__|| __|\ \/ /
 | (__| (_) || || | | || |_) || (_) || |   | |_  >  <
  \___|\___/ |_||_| |_|| .__/  \___/ |_|    \__|/_/\_\
                       | |
                       |_|
                       
```                      

Coinport ME can run on one single machine, or it can be deployed into many machines in various ways. You need to understand Akka cluster in order to scale up the application. Once you know how to do that, you simply need to change the config file `akka.conf`, no code modifiction is required.


#Frameworks

Our match engine was designed upon 2 very important concepts: [event-sourcing](http://www.martinfowler.com/eaaDev/EventSourcing.html) and actor model. We use [Typesafe](http://typesafe.com)'s [Akka](http://akka.io) platform to build our ME to make it super scalable and reactive. In order to understand the core logics, you need to know Scala and Akka pretty well. Luckily, both Scala and Akka are very well documented, and there are many tutorials out online. We hereby strongly recommand you read the documentation of Akka's Persistent module: [http://doc.akka.io/docs/akka/2.3.3/scala/persistence.html](http://doc.akka.io/docs/akka/2.3.3/scala/persistence.html).

#Important Modules
There are many source files in this repository. But the ones that really matter are briefly mentioned here. We suggest you focus on these files.

###MarketProcessor
Code in [coinex/coinex-backend/src/main/scala/com/coinport/coinex/markets](https://github.com/coinport/MatchEngine/tree/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/markets) directory are the core logics for submitting/cancelling orders, matching orders (generatign transactions). So it's the most important part of the project. [MarketProcess](https://github.com/coinport/MatchEngine/blob/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/markets/MarketProcessor.scala) class handles event-sourcing level messaging, and [MarketManager](https://github.com/coinport/MatchEngine/blob/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/markets/MarketManager.scala) class do the core ME job. If there is one file you want to start reading to learn how we match orders, you should start with MarketManager.scala.

###AccountProcessor
Code in [coinex/coinex-backend/src/main/scala/com/coinport/coinex/accounts](https://github.com/coinport/MatchEngine/tree/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/accounts) do all the accouting work, all user balances, fee calculations are done by [AccountManager](https://github.com/coinport/MatchEngine/blob/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/accounts/AccountManager.scala) class.

###UserProcessor

[UserProcessor](https://github.com/coinport/MatchEngine/blob/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/users/UserProcessor.scala) and [UserManager](https://github.com/coinport/MatchEngine/blob/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/users/UserManager.scala) and other files in [coinex/coinex-backend/src/main/scala/com/coinport/coinex/users](https://github.com/coinport/MatchEngine/tree/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/users) handle user credentials and profiles.

###BitwayProcessor
[BitwayProcessor](https://github.com/coinport/MatchEngine/tree/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/bitway) bridges cryptocurrency networks with the system. Basically it mirrors crytocurrency balances and handle withdrawals and deposits.


###ExportOpendataProcessor
Files inside [coinex/coinex-backend/src/main/scala/com/coinport/coinex/opendata](https://github.com/coinport/MatchEngine/tree/master/coinex/coinex-backend/src/main/scala/com/coinport/coinex/opendata) are responsible for exporting data to mongodb and files. This achieves exceptional openness and transparenecy. Data exported to mongodb are browsable online, data exported to files are downloadable.


#Questions?
If you have questions, feel free to drop us a line: [feedback@coinport.com](mailto:feedback@coinport.com). We will also post FAQs here later.