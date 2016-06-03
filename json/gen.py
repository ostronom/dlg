#!/usr/bin/env python

from json import dumps
from random import random, randint, choice


ids = range(1000)

def gen_key():
    return 'field_%d' % randint(1,100000)

def gen_value():
    g = [
	lambda: random(),
	lambda: randint(1,10000),
	lambda: gen_object(False)
    ]
    return choice(g)()

def gen_object(with_id=True):
    obj = {}
    if with_id:
	obj['id'] = choice(ids)
    while random() > 0.5:
	obj[gen_key()] = gen_value()
    return obj

def gen_file(n, entries):
    with open('%d.json' % n, 'w') as target:
	target.write('[%s]' % ','.join(
	    dumps(entry) for entry in (gen_object() for _ in xrange(entries))
	))

if __name__ == '__main__':
    for n in xrange(randint(10,100)):
	gen_file(n+1, randint(10000, 100000))