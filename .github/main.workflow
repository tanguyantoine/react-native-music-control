workflow "Build, Test, and Publish" {
  on = "push"
  resolves = [
    "Publish",
    "new-action",
  ]
}

action "Build" {
  uses = "actions/npm@master"
  args = "install"
}

action "Test" {
  needs = "Build"
  uses = "actions/npm@master"
  args = "test"
}

action "Filter release tag" {
  uses = "actions/bin/filter@b2bea07"
  args = "tag release-*"
}

action "GitHub Action for npm" {
  uses = "actions/npm@e7aaefe"
  needs = ["Filter release tag"]
  args = "install"
}

action "GitHub Action for npm-1" {
  uses = "actions/npm@e7aaefe"
  needs = ["GitHub Action for npm"]
  args = "test"
}

action "new-action" {
  uses = "owner/repo/path@ref"
  needs = ["GitHub Action for npm-1"]
}//action "Publish" {
//  needs = "Test"
//  uses = "actions/npm@master"
//  args = "publish --access public"
//  secrets = ["NPM_AUTH_TOKEN"]
//}
