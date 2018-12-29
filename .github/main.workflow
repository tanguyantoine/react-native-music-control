workflow "Build, Test, and Publish" {
  on = "push"
  resolves = [
    "Publish",
    "run tests",
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

action "install" {
  uses = "actions/npm@e7aaefe"
  needs = ["Filter release tag"]
  args = "install"
}

action "run tests" {
  uses = "actions/npm@e7aaefe"
  args = "test"
  needs = ["install"]
}

//action "Publish" {


//  needs = "Test"
//  uses = "actions/npm@master"
//  args = "publish --access public"
//  secrets = ["NPM_AUTH_TOKEN"]
//}
