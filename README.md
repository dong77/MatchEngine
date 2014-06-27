#About

Coinport is a fully open and transparent cryptocurrency exchange (https://coinport.com). This repository contains all core logic and code for its match engine (ME).

#Compile, Test, and Run

Coinport's ME use Scala as the main language. In order to compile the code, you need to have sbt installed. Please checkout sbt's official site for more information: http://www.scala-sbt.org. All following commands should be run inside the top level directory named `coinex`.

- To compile the code, run `sbt compile`.
- To run all the tests, run `sbt test`. No that you need to have network access as the testing framework will try to download the latest version of MongoDB to store system events and states.
- To start the ME, you need to have a MongoDB started on its default port, then run:

```sbt "project coinex-backend" "runMain com.coinport.coinex.CoinexApp 25551 127.0.0.1:25551 * 127.0.0.1 keyconfig"```

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


#Frameworks

The match engine is designed with 2 very important concepts: event-sourced and actor model. We use Typesafe's AKKA platform to build our ME to make it super scalable and reactive. In order to understand the core logics, you need to know Scala and Akka pretty well. Luckily, both Scala and Akka are very well documented, and there are many tutorials out online. 

