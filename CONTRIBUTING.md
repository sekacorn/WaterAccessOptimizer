# Contributing to WaterAccessOptimizer

Thank you for your interest in contributing to WaterAccessOptimizer! This project aims to improve water access for 2.2 billion people worldwide, and your contributions make a real difference.

## Code of Conduct

By participating in this project, you agree to maintain a respectful, inclusive, and collaborative environment.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/yourorg/WaterAccessOptimizer/issues)
2. If not, create a new issue with:
   - Clear, descriptive title
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots if applicable
   - Environment details (OS, browser, versions)

### Suggesting Enhancements

1. Check existing enhancement requests
2. Create an issue with:
   - Clear description of the enhancement
   - Use cases and benefits
   - MBTI considerations (if UI/UX related)
   - Potential implementation approach

### Pull Requests

1. **Fork the repository**
   ```bash
   git clone https://github.com/yourorg/WaterAccessOptimizer.git
   cd WaterAccessOptimizer
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**
   - Follow existing code style
   - Add comprehensive comments
   - Write tests (aim for >90% coverage)
   - Update documentation

4. **Test your changes**
   ```bash
   # Backend tests
   cd backend/water-integrator
   mvn test

   # Frontend tests
   cd frontend
   npm test

   # AI service tests
   cd ai-model
   pytest
   ```

5. **Commit with clear messages**
   ```bash
   git commit -m "feat: Add water quality threshold alerts

   - Implemented threshold monitoring
   - Added email notifications
   - Updated UI with alert indicators
   - Tests: 95% coverage"
   ```

6. **Push and create PR**
   ```bash
   git push origin feature/your-feature-name
   ```

## Development Guidelines

### Code Style

**Java (Spring Boot)**
- Follow Google Java Style Guide
- Use Checkstyle for validation
- Maximum line length: 120 characters
- Comprehensive JavaDoc comments

**JavaScript/React**
- ESLint with recommended rules
- Functional components with hooks
- PropTypes for type checking
- JSDoc comments for complex functions

**Python (AI Service)**
- PEP 8 style guide
- Flake8 for validation
- Type hints where appropriate
- Docstrings for all functions/classes

### Commit Message Format

Use conventional commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting)
- `refactor:` Code refactoring
- `test:` Adding/updating tests
- `chore:` Maintenance tasks

### Testing Requirements

- **Unit tests**: >90% coverage
- **Integration tests**: Critical paths covered
- **E2E tests**: Main user flows tested
- **Security tests**: OWASP compliance

### Documentation

- Update README.md for new features
- Add API documentation for new endpoints
- Include MBTI considerations for UI changes
- Provide usage examples

## MBTI Usability Guidelines

When contributing UI/UX changes, consider all 16 MBTI types:

- **Strategic types (ENTJ, INTJ, ESTJ)**: Provide metrics, analysis, structured data
- **Creative types (INFP, ENFP, ISFP)**: Use visual elements, narratives, values
- **Action types (ESTP, ESFP, ENTP)**: Quick actions, dynamic feedback, exploration
- **Supportive types (ESFJ, ISFJ, INFJ)**: Warmth, community focus, guidance

## Security Considerations

- Never commit sensitive data (API keys, passwords)
- Validate all user inputs
- Follow OWASP security guidelines
- Report security issues privately to security@wateraccessoptimizer.org

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Questions?

- Open a discussion on GitHub
- Email: contributors@wateraccessoptimizer.org
- Join our Discord/Slack community

Thank you for helping us improve water access worldwide! 🌍💧
