# Modifications copyright (C) 2021 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

"""Implements the ScoringProtocol, which runs all specified tests
associated with an assignment.
"""
import json

from client.sources.common import models as sources_models
from client.sources.ok_test import models as ok_test_models
from client.protocols.common import models as protocol_models
from client.utils import format
from collections import OrderedDict
import logging
from multiprocessing import Pool

log = logging.getLogger(__name__)


#####################
# Scoring Mechanism #
#####################

class ScoringProtocol(protocol_models.Protocol):
    """A Protocol that runs tests, formats results, and reports a student's
    score.
    """

    def run(self, messages, env=None):
        """Score tests and print results. Tests are taken from
        self.assignment.specified_tests. A score breakdown by question and the
        total score are both printed.

        ENV is used by the programatic API for Python doctests only.
        """
        if not self.args.score or self.args.testing:
            return

        format.print_line('~')
        print('Scoring tests')
        print()

        raw_scores = OrderedDict()
        for test in self.assignment.specified_tests:
            assert isinstance(test, sources_models.Test), 'ScoringProtocol received invalid test'

            log.info('Scoring test {}'.format(test.name))
            # A hack that allows programmatic API users to plumb a custom
            # environment through to Python tests.
            # Use type to ensure is an actual OkTest and not a subclass
            if type(test) == ok_test_models.OkTest:
                with Pool(1) as pool:
                    score, tle = test.score(env=env, pool=pool)
            else:
                score, tle = test.score(), False

            raw_scores[test.name] = (score, test.points, tle)

        messages['scoring'] = display_breakdown(raw_scores, self.args.score_out)
        print()


def display_breakdown(scores, outfile=None):
    """Writes the point breakdown to `outfile` given a dictionary of scores.
    `outfile` should be a string.  If `outfile` is None, write to stdout.

    RETURNS:
    dict; 'Total' -> finalized score (float)
    """
    total = 0
    if outfile:
        out = {'score': 0, 'details': []}
        for name, (score, _, tle) in scores.items():
            total += score
            out['score'] += int(score)
            out['details'].append({
                'title': name,
                'score': int(score),
                'message': 'TLE' if tle else None
            })
        with open(outfile, mode='w+', encoding='utf8') as out_fp:
            json.dump(out, out_fp, ensure_ascii=False)
    else:
        format.print_line('-')
        print('Point breakdown')
        for name, (score, max_score, _) in scores.items():
            print('    {}: {}/{}'.format(name, score, max_score))
            total += score
        print()
        print('Score:')
        print('    Total: {}'.format(total))
    return {'Total': total}


protocol = ScoringProtocol
