insert into ${schema}s_auditlog
(
	auditlog_id,
	operation,
	target_table,
	userlogin,
	extra_info,
	op_date,
	op_time,
	area,
	pkey,
	context_alias
)
values
(
	${seq:nextval@${schema}seq_auditlog},
	${fld:operation},
	${fld:target_table},
	'${def:user}',
	${fld:extra_info},
	{d '${def:date}'},
	'${def:time}',
	${fld:area},
	${fld:pkey},
	'${def:alias}'	
)