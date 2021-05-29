import argparse
import time

parser = argparse.ArgumentParser(description='external exec tester')
parser.add_argument('-d','--delay', type=int, help='A delay before result is returned')
parser.add_argument('-r','--result', type=str, help='The resulting string')

args = parser.parse_args()

time.sleep(args.delay)
print( "python says: " + args.result)
