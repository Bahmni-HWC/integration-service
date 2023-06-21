insert into mapping_group (name, integration_system_id) values ('User', (select id from integration_system where name = 'bahmni'));
insert into mapping_type (name, integration_system_id) values ('ProviderUUID', (select id from integration_system where name = 'bahmni'));
