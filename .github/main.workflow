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
  secrets = ["GITHUB_TOKEN"]
}

action "Install" {
  uses = "docker://node:10"
  needs = ["Filter release tag"]
  runs = "yarn"
  args = "install"
  secrets = ["GITHUB_TOKEN"]
}

action "Test" {
  uses = "docker://node:10"
  runs = "yarn"
  args = "test"
  needs = ["Install"]
  secrets = ["GITHUB_TOKEN"]
}

action "Release" {
  uses = "docker://node:10"
  needs = ["Test", "Install"]
  runs = "yarn"
  args = "semantic-release:ci"
  secrets = ["GITHUB_TOKEN", "NPM_TOKEN", "NPM_AUTH_TOKEN"]
  env = {
    npm_config_unsafe_perm = "true"
  }
}
