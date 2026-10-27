# TFC Overdims 開発ガイド

製品要求と Acceptance Criteria の正本は `PROJECT.md`。計画は
`.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
現在のコードは MDK の雛形であり、製品機能の実装済みとは扱わない。

## Harness runtime

- Minecraft 1.21.1、Java 21、NeoForge 21.1.264 を使用する。
  NeoForge の現在値は `gradle.properties` の `neo_version` に従う。
- runtime は Git 管理外の `.harness-state/runtime/` に置く。
  MC Pilot CLI は `pilot/node_modules/.bin/mct`、専用サーバーは `server/`。
- MC Pilot の環境変数は `MCT_HOME=$PWD/.harness-state/runtime/mct-home`、
  `MCT_CACHE_DIR=$PWD/.harness-state/runtime/mct-cache`、`MCT_SKILL_TARGETS=none`。
- 操作用クライアント名は `tfcoverdims-a`。サーバーは
  `127.0.0.1:25579`、検証ワールドは `harness-world`。
  Harness は E2E ごとにこのワールドを再作成する。
- TFC 4.2.12 と、その JAR metadata が要求する必須依存をサーバーと
  クライアントの `mods/` に配置する。Beneath は実行時依存にしない。
- `npm --prefix .harness run build` で Harness をビルドし、
  `node .harness/dist/cli/main.js doctor` と `validate` で準備を確認する。
  `plan` は Claude CLI の認証が利用できる環境で実行し、計画と進捗を commit する。
- 製品コードを変更した場合の完了確認は `./gradlew build`。
  runtime 準備だけで製品の GameTest / E2E 成功とは扱わない。
