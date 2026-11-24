import json
from pathlib import Path
import tempfile
import unittest
from apex_backend import Config, Gateway


class GatewayTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.calls = []
        self.result = {"analysis": "Fresh tyres may trade grip for time loss. Team intent is unknown.", "citations": ["timing"]}
        self.gateway = Gateway(Config(token="test-access", model_url="https://model.example/v1/chat/completions", model_key="secret", model="test-model", cache_path=str(Path(self.folder.name) / "cache.db")), self.fetch)

    def tearDown(self):
        self.gateway.close()
        self.folder.cleanup()

    def fetch(self, url, **kwargs):
        self.calls.append((url, kwargs))
        if "jolpi" in url:
            return {"MRData": {"StandingsTable": {"StandingsLists": [{"season": "2026", "round": "9", "DriverStandings": [{"Driver": {"driverId": "example", "givenName": "A", "familyName": "Driver"}, "points": "12.5", "position": "1", "wins": "0", "Constructors": [{"name": "Team"}]}]}]}}}
        return {"choices": [{"message": {"content": json.dumps(self.result)}}]}

    def body(self):
        return {"question": "Why pit?", "evidence": [{"id": "timing", "kind": "Observed", "text": "Exhibition sample: tyre age 12 laps."}]}

    def test_standings_are_normalized_and_cached_without_extra_upstream_calls(self):
        first = self.gateway.normalized_standings(2026)
        second = self.gateway.normalized_standings(2026)
        self.assertEqual(12.5, first["drivers"][0]["points"])
        self.assertFalse(first["cached"])
        self.assertTrue(second["cached"])
        self.assertEqual(1, len(self.calls))

    def test_authorization_requires_exact_token(self):
        self.assertTrue(self.gateway.authorized("Bearer test-access"))
        self.assertFalse(self.gateway.authorized("Bearer other"))
        self.assertFalse(self.gateway.authorized(""))

    def test_model_result_is_always_an_unverified_interpretation(self):
        answer = self.gateway.explain(self.body())
        self.assertEqual("ai_interpretation_unverified", answer["classification"])
        self.assertEqual(["timing"], answer["citations"])
        self.assertNotIn("secret", json.dumps(answer))

    def test_unknown_citations_are_rejected(self):
        self.result["citations"] = ["invented-source"]
        with self.assertRaises(ValueError):
            self.gateway.explain(self.body())

    def test_invalid_evidence_is_rejected_before_any_model_call(self):
        body = self.body()
        body["evidence"][0]["kind"] = "Definitely true"
        with self.assertRaises(ValueError):
            self.gateway.explain(body)
        self.assertEqual([], self.calls)

    def test_request_throttle_bounds_model_calls(self):
        for _ in range(12):
            self.gateway.explain(self.body())
        with self.assertRaises(RuntimeError):
            self.gateway.explain(self.body())
        self.assertEqual(12, len(self.calls))

    def test_missing_model_never_returns_a_fabricated_ai_answer(self):
        self.gateway.config.model_key = ""
        with self.assertRaises(RuntimeError):
            self.gateway.explain(self.body())
        self.assertEqual([], self.calls)


if __name__ == "__main__":
    unittest.main()
