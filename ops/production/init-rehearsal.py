#!/usr/bin/env python3
"""Create fresh, local-only credentials; refuses reuse or production endpoints."""
import os
import pathlib
import secrets
import sys

os.umask(0o077)
target = pathlib.Path(sys.argv[1]).resolve()
target.mkdir(mode=0o700, parents=False, exist_ok=False)
password = secrets.token_hex(32)
(target / 'mysql-password').write_text(password)
(target / 'client.cnf').write_text(f'[client]\nuser=root\npassword={password}\nhost=localhost\n')
(target / 'flyway.conf').write_text(
    f'flyway.url=jdbc:mysql://mysql:3306/lifeasgame?allowPublicKeyRetrieval=true&useSSL=false\n'
    f'flyway.user=root\nflyway.password={password}\nflyway.cleanDisabled=true\n'
    'flyway.baselineOnMigrate=false\nflyway.validateMigrationNaming=true\n')
(target / 'app.env').write_text(
    f'DB_URL=jdbc:mysql://mysql:3306/lifeasgame?allowPublicKeyRetrieval=true&useSSL=false\n'
    f'DB_USERNAME=root\nDB_PASSWORD={password}\nJWT_SECRET={secrets.token_hex(64)}\n')
print('Created private local rehearsal inputs; no production credentials copied.')
