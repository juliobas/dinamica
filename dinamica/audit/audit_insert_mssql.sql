insert into ${schema}s_auditlog
(
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