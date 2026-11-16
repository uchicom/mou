# CLAUDE.md — mou プロジェクト

## 概要

**mou** は都市・物流・インフラのシミュレーションエンジン。MVP は物流シミュレーションのみ（GUI・DB なし）。
設計は [docs/design.md](docs/design.md) が正。実装は GitHub Issue 単位で進める。

## 守ること

- Java 25 + Maven。コアの runtime 依存は JDK のみ（テストのみ JUnit + AssertJ）
- 1 tick = 1 時間。個体（車両・人）は動かさず、量を数式で計算する
- 物資量は `long`（kg）。加減乗算は exact 演算（オーバーフローはシミュレーション失敗）
- 反復順序を決定的にする（HashMap の反復順に依存しない）。同一設定なら同一結果。MVP は乱数を使わない
- コアは描画を知らない
- 設計書にない機能・過剰な抽象化を足さない。設計上の重要な判断は ADR として設計書に記録する
- コードを追加・変更したら対応する JUnit テストを追加する

## ブランチ・Issue

- ブランチ名は `uchicom/<Issue 番号>`。reopen 後の修正は `uchicom/<番号>a`, `b` …
- 完了時に Issue へテスト結果をコメントする

## コマンド

```bash
mvn spotless:apply clean compile verify   # 通常はこれ
mvn exec:java
```

## 規約

- フォーマットは Spotless に従う（手動で変えない）
- Error Prone の警告は残さない
- `com.uchicom.mou` 配下は JDK と mou 自身以外を import しない（`DependencyDirectionTest` で検証）
