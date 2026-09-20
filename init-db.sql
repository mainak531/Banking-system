-- Banking System — Database Initialization Script
-- This file is automatically executed by MySQL on first container startup.
-- It creates all databases required by the microservices.

CREATE DATABASE IF NOT EXISTS `accountdb`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `transactiondb`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `paymentdb`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to root for local development convenience
GRANT ALL PRIVILEGES ON `accountdb`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `transactiondb`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `paymentdb`.* TO 'root'@'%';
FLUSH PRIVILEGES;
