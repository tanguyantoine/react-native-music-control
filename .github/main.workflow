workflow "Build, Test, and Publish" {
  on = "release"
  resolves = [
    "Filter release tag",
    "GitHub Action for npm",
  ]
}

action "Filter release tag" {
  uses = "actions/bin/filter@b2bea07"
  args = "tag release-*"
}

action "Install" {
  uses = "actions/npm@e7aaefe"
  needs = ["Filter release tag"]
  args = "install"
}

action "Test" {
  uses = "actions/npm@e7aaefe"
  args = "test"
  needs = ["Install"]
}

action "GitHub Action for npm" {
  uses = "actions/npm@e7aaefe"
  needs = ["Install"]
  args = "test"
}
