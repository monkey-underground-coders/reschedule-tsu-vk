# reschedule-tsu-vk
Unofficial vk bot for TvSU schedule, vk.com endpoint. 
This bot works with [reschedule-tsu-spring](https://github.com/monkey-underground-coders/reschedule-tsu-spring).  
Built using [VK Java SDK](https://github.com/VKCOM/vk-java-sdk), [Spring Boot](https://spring.io), [DialogFlow](https://dialogflow.com) and [Sentry](https://sentry.io).

## Features
- Schedule for current/next/any day (feat. DialogFlow)
- Schedule for seven days forward and a whole week
- Teacher's schedule
- Past/Live/Future lessons indication
- Subgroups support

## Setup

0) Create a group on [vk.com](https://vk.com), enable messages for bot and create a token with messaging and admin
   privileges
1) Deploy [reschedule-tsu-spring](https://github.com/monkey-underground-coders/reschedule-tsu-spring) or contact
   info@a6raywa1cher.com for the deployed server. Also, deploy PostgreSQL or other hibernate compatible DB
2) Clone repository, fill application.yml
3) Using Maven, package it
4) Run :)

Or use [reschedule-tsu-docker](https://github.com/monkey-underground-coders/reschedule-tsu-docker)

## Contribution

Any feature requests, improvements, bug and security reports are welcome! Leave it here or email me:
info@a6raywa1cher.com
