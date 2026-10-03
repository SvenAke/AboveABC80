#!/usr/bin/env python3

import argparse
import sys
from tzxlib.tzxfile import TzxFile
from tzxlib.saver import TapeSaver

def print_pulses(tzx, clock, verbose):
    """
    Skriver ut längden av varje puls i t-states och antal lika pulser.
    :param tzx: TzxFile-objekt.
    :param clock: CPU-klockfrekvens i Hz.
    :param verbose: Om True, skriv ut blockinformation.
    """
    saver = TapeSaver(clock, tzx.return_tstates_directly)
    previous_pulse = None
    count = 0
    for block_index, block in enumerate(tzx.blocks):
        if verbose:
            print(f"Block {block_index}: {block.type}")
        for pulses in block.playback(saver):
            if pulses > 0:
                if pulses == previous_pulse:
                    count += 1
                else:
                    if previous_pulse is not None:
                        print(f"{previous_pulse:<10}:{count}")
                    previous_pulse = pulses
                    count = 1
    # Print the last pulse count if any
    if previous_pulse is not None:
        print(f"{previous_pulse:<10}:{count}")

def main():
    parser = argparse.ArgumentParser(description='Print pulse lengths in t-states from a TZX file')
    parser.add_argument('file',
                        nargs='?',
                        type=argparse.FileType('rb'),
                        default=(None if sys.stdin.isatty() else sys.stdin.buffer),
                        help='TZX file, stdin if omitted')
    parser.add_argument('-c', '--clock',
                        dest='clock',
                        default=3500000,
                        type=int,
                        help='Reference Z80 CPU clock, in Hz (default: 3.5 MHz)')
    parser.add_argument('-v', '--verbose',
                        dest='verbose',
                        action='store_true',
                        help='Be verbose about what you are doing')
    args = parser.parse_args()

    if args.file is None:
        parser.print_help(sys.stderr)
        sys.exit(1)

    tzx = TzxFile(True)
    tzx.read(args.file)

    print_pulses(tzx, args.clock, args.verbose)

if __name__ == "__main__":
    main()