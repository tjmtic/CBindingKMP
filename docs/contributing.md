# Contributing to CBindingKMP

We welcome contributions of all kinds! This guide will help you get started with contributing to CBindingKMP.

## Ways to Contribute

1.  **Report Bugs**: If you find a bug, please create a GitHub issue with a detailed description and steps to reproduce.
2.  **Suggest Features**: Have an idea for a new feature? Open an issue to discuss it.
3.  **Submit Pull Requests**: Fix a bug, add a feature, or improve documentation.

## Development Setup

1.  **Fork the Repository**: Create your own fork of CBindingKMP.
2.  **Clone Locally**: `git clone https://github.com/your-username/CBindingKMP.git`
3.  **Create a Branch**: `git checkout -b feature/my-new-feature`
4.  **Set Up Environment**: Ensure you have the [Prerequisites](getting-started.md#prerequisites) installed.

## Code Style

-   **Kotlin**: Follow the [official Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html).
-   **C/C++**: Use consistent indentation (4 spaces) and descriptive naming.
-   **Gradle**: Keep build scripts clean and use the [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html).

## Testing Your Changes

Before submitting a pull request, ensure all tests pass:

```bash
./gradlew :shared:allTests
```

## Pull Request Process

1.  **Update Documentation**: If your change adds a new feature or modifies existing behavior, update the relevant files in the `docs/` directory.
2.  **Write Tests**: Add unit tests for your changes where applicable.
3.  **Self-Review**: Double-check your code for clarity and potential bugs.
4.  **Submit PR**: Create a pull request against the `main` branch. Provide a clear description of your changes.

## Community

Join our community to discuss development and get help:
-   **GitHub Discussions**: [link-to-discussions]
-   **Slack/Discord**: [link-to-chat]

Thank you for contributing to CBindingKMP!
