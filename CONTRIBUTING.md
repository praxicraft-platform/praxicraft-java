# Contributing

```bash
git clone https://github.com/praxicraft-platform/praxicraft-java.git
cd praxicraft-java
mvn test
```

Guidelines:

- Thin wrapper around the [Assess Public API](https://docs.praxicraft.com).
- Keep HTTP mocked in tests — no live production calls in CI.
- Release notes: [RELEASING.md](RELEASING.md).
