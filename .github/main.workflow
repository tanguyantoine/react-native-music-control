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
  args = "tag release-*"
}

action "Install" {
  uses = "docker://node:10"
  needs = ["Filter release tag"]
  runs = "yarn"
  args = "install"
}

action "Test" {
  uses = "docker://node:10"
  runs = "yarn"
  args = "test"
  needs = ["Install"]
}

action "Release" {
  uses = "docker://node:10"
  needs = ["Test", "Install"]
  runs = "yarn"
  args = "semantic-release"
  secrets = ["GITHUB_TOKEN", "NPM_TOKEN", "NPM_AUTH_TOKEN"]
}
