module.exports = {
  apps: [
    {
      name: "harbourfront",
      script: "env.sh",
      interpreter: "bash",
      args: "java -jar server/target/harbourfront-server-1.0.jar",
      autorestart: true,
      watch: false,
      max_memory_restart: "1G",
    },
  ],
};
