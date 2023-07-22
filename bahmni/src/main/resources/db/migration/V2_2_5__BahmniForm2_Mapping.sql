insert into mapping_type (name, integration_system_id) values ('BahmniForm2Name', (select id from integration_system where name = 'bahmni'));
