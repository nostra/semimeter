# SemiMeter

 SemiMeter shall offer a blank gif over http, and store this information
 in a manner that facilitates extraction of performance related data.

~~~~~~~~~~~~~~
# Database setup
~~~~~~~~~~~~~~

 SemiMeter would like a separate database, with the username
 semimeter and the expected password.

## Creating the mysql database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    mysql --user=root --password=something
    create database semimeter character set utf8; 
    -- Add user
    grant all privileges on semimeter.* to semimeter@'localhost' identified by 'semimeter123' ;
    FLUSH PRIVILEGES;

