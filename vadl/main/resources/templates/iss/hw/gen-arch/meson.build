
gen_arch_ss = ss.source_set()
gen_arch_ss.add(files('boot.c'))
gen_arch_ss.add(files('[(${gen_machine_lower})].c'))

hw_arch += {'[(${gen_arch})]': gen_arch_ss}
