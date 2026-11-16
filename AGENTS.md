# AGENTS.md — mou プロジェクト AI 開発ルール

Claude、ChatGPT、Gemini、GitHub Copilot など全 AI ツール共通のルール。コードの生成・編集・レビューは本ファイルに従う。

## プロジェクト概要

**mou** は都市・物流・インフラのシミュレーションエンジン。MVP は物流シミュレーションのみ（GUI・DB なし）。
設計は [docs/design.md](docs/design.md) が正。

## 技術スタック

| カテゴリ | 技術 |
|---|---|
| 言語 | Java 25 |
| ビルド | Maven |
| 単体テスト | JUnit + AssertJ |
| カバレッジ | JaCoCo |
| 静的解析 | Error Prone |
| フォーマッタ | Spotless |

## ルール

- コアの runtime 依存は JDK のみ。`com.uchicom.mou` 配下は JDK と mou 自身以外を import しない
- 1 tick = 1 時間。物資量は `long`（kg）で exact 演算。乱数は MVP では使わない
- 反復順序を決定的にし、同一設定なら同一結果にする
- 設計書にない機能追加・過剰な抽象化をしない。コードを変更したらテストも追加する
- Spotless のフォーマットを手動で変えない。Error Prone の警告を残さない
- ブランチ名は `uchicom/<Issue 番号>`（reopen 後の修正は `uchicom/<番号>a` …）

## コマンド

```bash
mvn spotless:apply clean compile verify
```
