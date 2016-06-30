-- MySQL Script generated by MySQL Workbench
-- 06/30/16 22:28:41
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema uberbahn
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema uberbahn
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `uberbahn` DEFAULT CHARACTER SET utf8 ;
USE `uberbahn` ;

-- -----------------------------------------------------
-- Table `uberbahn`.`account`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `uberbahn`.`account` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `login` VARCHAR(100) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `secret` VARCHAR(100) NOT NULL,
  `firstName` VARCHAR(100) NOT NULL,
  `lastName` VARCHAR(200) NOT NULL,
  `dateOfBirth` DATE NOT NULL,
  `employee` TINYINT(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uc_appUser_login` (`login` ASC),
  UNIQUE INDEX `uc_appUser_email` (`email` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `uberbahn`.`route`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `uberbahn`.`route` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(100) NOT NULL,
  `timeOfDeparture` TIME NOT NULL,
  `pricePerMinute` DECIMAL(5,2) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `title` (`title` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `uberbahn`.`station`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `uberbahn`.`station` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(150) NOT NULL,
  `timezone` INT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `title` (`title` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `uberbahn`.`spot`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `uberbahn`.`spot` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `stationId` INT(11) NOT NULL,
  `routeId` INT(11) NOT NULL,
  `minutesSinceDeparture` INT NOT NULL,
  UNIQUE INDEX `uc_spot_routeTime` (`routeId` ASC, `minutesSinceDeparture` ASC),
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uc_route_station` (`stationId` ASC, `routeId` ASC),
  CONSTRAINT `fk_spot_route`
    FOREIGN KEY (`routeId`)
    REFERENCES `uberbahn`.`route` (`id`),
  CONSTRAINT `fk_spot_station`
    FOREIGN KEY (`stationId`)
    REFERENCES `uberbahn`.`station` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `uberbahn`.`train`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `uberbahn`.`train` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `routeId` INT(11) NOT NULL,
  `dateOfDeparture` DATE NOT NULL,
  `numberOfSeats` INT(11) NOT NULL,
  `priceCoefficient` DOUBLE NOT NULL,
  `archived` TINYINT(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uc_train_routeDate` (`routeId` ASC, `dateOfDeparture` ASC),
  CONSTRAINT `fk_train_route`
    FOREIGN KEY (`routeId`)
    REFERENCES `uberbahn`.`route` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `uberbahn`.`ticket`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `uberbahn`.`ticket` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `trainId` INT(11) NOT NULL,
  `firstName` VARCHAR(100) NOT NULL,
  `lastName` VARCHAR(200) NOT NULL,
  `dateOfBirth` DATE NOT NULL,
  `stationOfDepartureId` INT(11) NOT NULL,
  `stationOfArrivalId` INT(11) NOT NULL,
  `datetimeOfPurchase` DATETIME NOT NULL,
  `accountId` INT(11) NOT NULL,
  `price` DECIMAL(8,2) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_ticket_train` (`trainId` ASC),
  INDEX `fk_ticket_stationOfDeparture` (`stationOfDepartureId` ASC),
  INDEX `fk_ticket_stationOfArrival` (`stationOfArrivalId` ASC),
  INDEX `fk_ticket_user` (`accountId` ASC),
  UNIQUE INDEX `uc_passenger_train` (`trainId` ASC, `firstName` ASC, `lastName` ASC, `dateOfBirth` ASC),
  CONSTRAINT `fk_ticket_stationOfArrival`
    FOREIGN KEY (`stationOfArrivalId`)
    REFERENCES `uberbahn`.`station` (`id`),
  CONSTRAINT `fk_ticket_stationOfDeparture`
    FOREIGN KEY (`stationOfDepartureId`)
    REFERENCES `uberbahn`.`station` (`id`),
  CONSTRAINT `fk_ticket_train`
    FOREIGN KEY (`trainId`)
    REFERENCES `uberbahn`.`train` (`id`),
  CONSTRAINT `fk_ticket_user`
    FOREIGN KEY (`accountId`)
    REFERENCES `uberbahn`.`account` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `uberbahn`.`presence`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `uberbahn`.`presence` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `trainId` INT NOT NULL,
  `spotId` INT NOT NULL,
  `instant` TIMESTAMP(6) NOT NULL,
  `numberOfTickets` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_presence_train_idx` (`trainId` ASC),
  INDEX `fk_presence_spot_idx` (`spotId` ASC),
  UNIQUE INDEX `uc_train_spot` (`trainId` ASC, `spotId` ASC),
  CONSTRAINT `fk_presence_train`
    FOREIGN KEY (`trainId`)
    REFERENCES `uberbahn`.`train` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_presence_spot`
    FOREIGN KEY (`spotId`)
    REFERENCES `uberbahn`.`spot` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE USER 'uberbahn_webapp' IDENTIFIED BY '123';

GRANT SELECT ON TABLE `mydb`.* TO 'uberbahn_webapp';
GRANT SELECT ON TABLE `uberbahn`.* TO 'uberbahn_webapp';
GRANT SELECT, INSERT, TRIGGER ON TABLE `mydb`.* TO 'uberbahn_webapp';
GRANT SELECT, INSERT, TRIGGER ON TABLE `uberbahn`.* TO 'uberbahn_webapp';
GRANT SELECT, INSERT, TRIGGER, UPDATE, DELETE ON TABLE `mydb`.* TO 'uberbahn_webapp';
GRANT SELECT, INSERT, TRIGGER, UPDATE, DELETE ON TABLE `uberbahn`.* TO 'uberbahn_webapp';

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
