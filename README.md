# mou

都市・物流・インフラをシミュレーションするゲームエンジン（シミュレーションコア）。

ビットマップを初期条件（Seed）として与え、道路・施設・産業を配置した世界が時間経過でどう変化するかを、
1 tick = 1 時間の数式ベースで計算する。MVP は物流シミュレーションに限定し、GUI は対象外。

- 設計: [docs/design.md](docs/design.md)
- 要件: JDK 25 / Maven

```bash
mvn spotless:apply clean compile verify   # フルビルド
mvn exec:java                             # 実行
```
