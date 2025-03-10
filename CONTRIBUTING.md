First off, thank you for considering contributing to HAL. It's people like you that make HAL such a great tool. HAL is an open source project, and we love to receive contributions from our community — you! There are many ways to contribute, from writing tutorials or blog posts, improving the documentation, submitting bug reports and feature requests or writing code which can be incorporated into HAL itself.

We expect all contributors and users to follow our [Code of Conduct](CODE_OF_CONDUCT.md) when communicating through project channels. These include, but are not limited to: chat, issues, code.

All recent development happens in the branch `main`. There are additional [branches](https://hal.github.io/development/branches/) mainly used for maintenance.

# One time setup

## Create a GitHub account

If you don't have one already, head to https://github.com/

## Fork HAL

Fork https://github.com/hal/console into your GitHub account.

## Clone your newly forked repository onto your local machine

```bash
git clone git@github.com:[your username]/console.git
cd console
```

## Add a remote reference to upstream

This makes it easy to pull down changes in the project over time

```bash
git remote add upstream git://github.com/hal/console.git
```

# Development Process

This is the typical process you would follow to submit any changes to HAL.

## Pulling updates from upstream

```bash
git pull --rebase upstream main
```

> Note that --rebase will automatically move your local commits, if you have
> any, on top of the latest branch you pull from.
> If you don't have any commits it is safe to leave off, but for safety it
> doesn't hurt to use it each time just in case you have a commit you've
> forgotten about!

## Discuss your planned changes (if you want feedback)

 * HAL Issue Tracker - https://issues.jboss.org/browse/HAL
 * Gitter - https://gitter.im/hal/console

## Create a simple topic branch to isolate your work (recommended)

```bash
git checkout -b my_cool_feature
```

## Make the changes

Make whatever code changes, including new tests to verify your change, are necessary and ensure that the build and tests pass. Make sure your code changes apply to the rules defined in [build-config](build-config/src/main/resources/etc). This should happen automatically, but you can also execute the `./format.sh` and `./validate.sh` scripts manually before submitting your changes.

```bash
mvn clean verify
```

> If you're making non code changes, the above step is not required.

## Commit changes

Add whichever files were changed into 'staging' before performing a commit:

```bash
git commit
```

## Rebase changes against main

Once all your commits for the issue have been made against your local topic branch, we need to rebase it against branch main in upstream to ensure that your commits are added on top of the current state of main. This will make it easier to incorporate your changes into the main branch, especially if there has been any significant time passed since you rebased at the beginning.

```bash
git pull --rebase upstream main
```

## Push to your repo

Now that you've sync'd your topic branch with upstream, it's time to push it to your GitHub repo.

```bash
git push origin my_cool_feature
```

## Getting your changes merged into upstream, a pull request

Now your updates are in your GitHub repo, you will need to notify the project that you have code/docs for inclusion.

 * Send a pull request, by clicking the pull request link while in your repository fork
 * After review a maintainer will merge your pull request, update/resolve associated issues, and reply when complete
 * Lastly, switch back to branch main from your topic branch and pull the updates

```bash
git checkout main
git pull upstream main
```

 * You may also choose to update your origin on GitHub as well

```bash
git push origin
```

## Some tips

Here are some tips on increasing the chance that your pull request is accepted:

 * Write a [good commit message](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)
 * Include tests that fail without your code, and pass with it

# Issues

HAL uses JIRA to manage issues. All issues can be found [here](https://issues.redhat.com/projects/HAL/issues).

To create a new issue, comment on an existing issue, or assign an issue to yourself, you'll need to first [create a JIRA account](https://issues.redhat.com/).

## Good First Issues

Want to contribute to HAL but aren't quite sure where to start? Check out our issues with the `good-first-issue` label. These are a triaged set of issues that are great for getting started on our project. These can be found [here](https://issues.redhat.com/issues/?filter=12402986).

Once you have selected an issue you'd like to work on, make sure it's not already assigned to someone else. To assign an issue to yourself, simply click on "Start Progress". This will automatically assign the issue to you.
