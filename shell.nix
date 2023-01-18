with import <nixpkgs> { };
stdenv.mkDerivation rec {
  name = "env";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  GS_USERNAME = "";
  GS_PASSWORD = "";
  GN_USERNAME = "";
  GN_PASSWORD = "";
  buildInputs = [
    maven
    postgresql_10
    (jdk8.overrideAttrs
      (_: { postPatch = "rm man; ln -s ../zulu-8.jdk/Contents/Home/man man"; }))
  ];

  postgresConf = writeText "postgresql.conf" ''
    # Add Custom Settings
    log_min_messages = warning
    log_min_error_statement = error
    log_min_duration_statement = 100  # ms
    log_connections = on
    log_disconnections = on
    log_duration = on
    #log_line_prefix = '[] '
    log_timezone = 'UTC'
    log_statement = 'all'
    log_directory = 'pg_log'
    log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
    logging_collector = on
    log_min_error_statement = error
  '';

  # Post Shell Hook
  shellHook = ''
    echo "Using ${postgresql_10.name}."
    export PGDATABASE="lch_dev";
    export PGDATABASE_TEST="lch_test";
    export PG="$PWD/.pg";
    export PGDATA="$PG/pgdata";
    export PGHOST="/tmp";
    # The port may coilde with other instances but the app doesn't let you change it
    export PGPORT="5432";
    export PGUSER="lch";

    # Setup: DB
    if [ ! -d $PGDATA ]; then
      echo "Starting ${postgresql_10.name}."
      initdb --username $PGUSER -D $PGDATA && cat "$postgresConf" >> $PGDATA/postgresql.conf 
      pg_ctl start -o "-p $PGPORT -c unix_socket_directories=/tmp" -D $PGDATA
      sleep 1
      psql -p $PGPORT -U $PGUSER -d postgres -c "CREATE database $PGDATABASE;"
      psql -p $PGPORT -U $PGUSER -c "grant all privileges on database $PGDATABASE to $PGUSER";
      sleep 1
      psql -p $PGPORT -U $PGUSER -d postgres -c "CREATE database $PGDATABASE_TEST;"
      psql -p $PGPORT -U $PGUSER -c "grant all privileges on database $PGDATABASE_TEST to $PGUSER";
      sleep 1
      pg_ctl stop
    fi

    '';
}
