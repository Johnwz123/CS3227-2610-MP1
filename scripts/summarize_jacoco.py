#!/usr/bin/env python3
"""Format a JaCoCo XML report as a GitHub-flavored Markdown coverage table."""

import argparse
from pathlib import Path
import xml.etree.ElementTree as element_tree


COUNTER_TYPES = ("INSTRUCTION", "BRANCH", "COMPLEXITY", "LINE", "METHOD", "CLASS")
HEADERS = (
    "Element",
    "Missed Instructions",
    "Cov.",
    "Missed Branches",
    "Cov.",
    "Missed Cxty",
    "Cxty",
    "Missed Lines",
    "Lines",
    "Missed Methods",
    "Methods",
    "Missed Classes",
    "Classes",
)


def metrics(element: element_tree.Element, counter_type: str) -> tuple[int, int]:
    """Return the missed and covered values for a JaCoCo counter type."""
    counter = next(
        (counter for counter in element.findall("counter") if counter.attrib["type"] == counter_type),
        None,
    )
    if counter is None:
        return 0, 0
    return int(counter.attrib["missed"]), int(counter.attrib["covered"])


def coverage(missed: int, covered: int) -> str:
    """Return JaCoCo-style whole-percent coverage, or n/a when unavailable."""
    total = missed + covered
    return f"{covered / total * 100:.0f}%" if total else "n/a"


def count(missed: int, covered: int) -> str:
    """Return the JaCoCo-style missed-of-total value."""
    return f"{missed:,} of {missed + covered:,}"


def table_row(element: element_tree.Element, name: str) -> list[str]:
    """Return one Markdown table row for a JaCoCo package or report."""
    values = {counter_type: metrics(element, counter_type) for counter_type in COUNTER_TYPES}
    instruction_missed, instruction_covered = values["INSTRUCTION"]
    branch_missed, branch_covered = values["BRANCH"]
    return [
        name,
        count(instruction_missed, instruction_covered),
        coverage(instruction_missed, instruction_covered),
        count(branch_missed, branch_covered),
        coverage(branch_missed, branch_covered),
        str(values["COMPLEXITY"][0]),
        str(sum(values["COMPLEXITY"])),
        str(values["LINE"][0]),
        str(sum(values["LINE"])),
        str(values["METHOD"][0]),
        str(sum(values["METHOD"])),
        str(values["CLASS"][0]),
        str(sum(values["CLASS"])),
    ]


def build_summary(report_path: Path) -> str:
    """Build the complete JaCoCo coverage summary from an XML report."""
    report = element_tree.parse(report_path).getroot()
    packages = sorted(
        report.findall("package"),
        key=lambda package: metrics(package, "INSTRUCTION")[0],
        reverse=True,
    )
    rows = [table_row(package, package.attrib["name"].replace("/", ".")) for package in packages]
    rows.append(table_row(report, "Total"))
    table = "\n".join(
        [
            "| " + " | ".join(HEADERS) + " |",
            "| " + " | ".join(["---"] * len(HEADERS)) + " |",
            *("| " + " | ".join(row) + " |" for row in rows),
        ]
    )
    return f"## JaCoCo code coverage\n\n{table}\n"


def append_to_file(path: Path, content: str) -> None:
    """Append UTF-8 content to an existing GitHub Actions output file."""
    with path.open("a", encoding="utf-8") as file:
        file.write(content)


def parse_arguments() -> argparse.Namespace:
    """Parse report and optional GitHub Actions output-file arguments."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--report",
        type=Path,
        default=Path("build/reports/jacoco/test/jacocoTestReport.xml"),
        help="path to the JaCoCo XML report",
    )
    parser.add_argument("--step-summary", type=Path, help="path to GITHUB_STEP_SUMMARY")
    parser.add_argument("--github-output", type=Path, help="path to GITHUB_OUTPUT")
    return parser.parse_args()


def main() -> None:
    """Print the summary locally and write it to GitHub Actions files when requested."""
    arguments = parse_arguments()
    summary = build_summary(arguments.report)

    if arguments.step_summary:
        append_to_file(arguments.step_summary, summary)
    if arguments.github_output:
        append_to_file(arguments.github_output, f"summary<<EOF\n{summary}EOF\n")
    if not arguments.step_summary and not arguments.github_output:
        print(summary, end="")


if __name__ == "__main__":
    main()
