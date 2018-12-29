workflow "Build, Test, and Publish" {
  on = "push"
  resolves = [
    "Filter release tag",
    "Install",
    "Test",
    "Release",
  ]
}

action "Filter release tag" {
  uses = "actions/bin/filter@b2bea07"
  args = "branch release-*"
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

action "Release" {
  uses = "actions/npm@e7aaefe"
  needs = ["Test", "Install"]
  args = "run semantic-release"
  secrets = ["GITHUB_TOKEN", "NPM_TOKEN", "NPM_AUTH_TOKEN"]
}
