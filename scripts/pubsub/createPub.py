filenames = ['sub_0', 'sub_1','sub_2', 'sub_3', 'sub_4', 'sub_5', 'sub_6', 'sub_7']
with open('pub', 'w') as outfile:
    for fname in filenames:
        with open(fname) as infile:
            for line in infile:
                outfile.write(line)
