module.exports = {
  apps: [
    {
      name: "harbourfront",
      script: "env.sh",
      interpreter: "bash",
      args: "java -jar -Djava.net.preferIPv6Addresses=true server/target/harbourfront-server-1.0.jar",
      autorestart: true,
      watch: false,
      max_memory_restart: "1G",
    },
  ],
};
