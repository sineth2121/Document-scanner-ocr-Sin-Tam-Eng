Build helper: use the included Maven wrapper

If you don't have Maven installed globally, use the provided `mvnw.cmd` (Windows) or `mvnw` (Unix) script. The wrapper will download a copy of Apache Maven into `.mvn/apache-maven` on first run.

Windows example (PowerShell / cmd):

```powershell
.\
\mvnw.cmd -v
.\mvnw.cmd -DskipTests package
```

Linux / macOS example:

```bash
./mvnw -v
./mvnw -DskipTests package
```

If the wrapper fails to download Maven, install Maven manually from https://maven.apache.org/download.cgi and ensure `mvn` is on your `PATH`.
