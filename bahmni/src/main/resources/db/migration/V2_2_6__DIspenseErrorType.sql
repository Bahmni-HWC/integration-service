insert into error_type (name, integration_system_id) values ('DispenseError', (select id from integration_system where name = 'bahmni'));
