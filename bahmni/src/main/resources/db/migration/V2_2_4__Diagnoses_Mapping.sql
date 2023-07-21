insert into mapping_type (name, integration_system_id) values ('DiagnosesEncounterType', (select id from integration_system where name = 'bahmni'));
insert into mapping_type (name, integration_system_id) values ('DiagnosesConcept', (select id from integration_system where name = 'bahmni'));
