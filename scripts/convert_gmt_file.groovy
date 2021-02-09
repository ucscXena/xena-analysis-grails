def cli = new CliBuilder(usage: 'convert_gmt_file.groovy <options>')
cli.setStopAtNonOption(true)
cli.inputfile('TSV file of GMT', required: true, args: 1)
cli.ourputfile('converted output TSV file of GMT', required: false, args: 1)
cli.mappingfile('gene mpapings', required: false, args: 1)
//cli.output('Admin password', required: false, args: 1)
//cli.password('Admin username', required: false, args: 1)

options = cli.parse(args)

println options
